package com.ai.assistance.operit.ui.features.chat.components.part

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Context
import com.ai.assistance.operit.R
import com.ai.assistance.operit.api.chat.EnhancedAIService
import com.ai.assistance.operit.data.translation.CachedTranslator
import com.ai.assistance.operit.data.translation.TranslationPrefetch
import com.ai.assistance.operit.ui.features.chat.components.compactDialogHeightWhenShort
import com.ai.assistance.operit.ui.features.chat.components.rememberCompactDialogMetrics

/** 工具执行结果显示组件 简洁风格，显示工具执行结果，无边框，与CompactToolDisplay风格一致 通过缩进和特殊图标区分工具调用和执行结果 支持点击查看详细内容 */
@Composable
fun ToolResultDisplay(
        toolName: String,
        result: String,
        isSuccess: Boolean = true,
        onCopyResult: () -> Unit = {},
        modifier: Modifier = Modifier,
        enableDialog: Boolean = true  // 新增参数：是否启用弹窗功能，默认启用
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val hasContent = result.isNotBlank()

    // 弹窗状态
    var showDetailDialog by remember { mutableStateOf(false) }

    // 显示详细内容的弹窗 - 仅在启用弹窗时显示
    if (showDetailDialog && enableDialog) {
        ToolResultDetailDialog(
                toolName = toolName,
                result = result,
                isSuccess = isSuccess,
                onDismiss = { showDetailDialog = false },
                onCopy = {
                    clipboardManager.setText(AnnotatedString(result))
                    onCopyResult()
                }
        )
    }

    val summaryText =
        if (hasContent) {
            result.take(200)
        } else if (isSuccess) {
            context.getString(R.string.execution_success)
        } else {
            context.getString(R.string.execution_failed)
        }
    val semanticResultText = remember(result, hasContent) {
        if (!hasContent) {
            ""
        } else {
            result
                .replace("\n", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .let { normalized ->
                    if (normalized.length <= 20) normalized else normalized.take(20) + "..."
                }
        }
    }
    val semanticDescription = remember(toolName, summaryText, semanticResultText, isSuccess, hasContent) {
        val resultLabel = context.getString(R.string.tool_execution_result)
        val statusLabel =
            if (isSuccess) context.getString(R.string.success) else context.getString(R.string.failed)
        if (hasContent && semanticResultText.isNotBlank()) {
            "$resultLabel: $toolName, $statusLabel, $semanticResultText"
        } else {
            "$resultLabel: $toolName, $statusLabel, $summaryText"
        }
    }

    CanvasToolResultRow(
        summary = summaryText,
        isSuccess = isSuccess,
        semanticDescription = semanticDescription,
        modifier = modifier,
        emphasizeSummary = !hasContent,
        onClick =
            if (hasContent && enableDialog) {
                { showDetailDialog = true }
            } else {
                null
            },
        onCopyClick =
            if (hasContent) {
                {
                    clipboardManager.setText(AnnotatedString(result))
                    onCopyResult()
                }
            } else {
                null
            },
    )
}

/**
 * One cached translation of runtime text, shared by the dialog above.
 * Untranslatable input yields null and the caller shows the source.
 */
suspend fun translateRuntimeText(context: Context, source: String): String? {
    val cached = CachedTranslator(
        cacheDir = context.filesDir,
        fetchFresh = { text ->
            EnhancedAIService.getInstance(context).translateText(text)
        },
    )
    return runCatching { cached.translate(source) }.getOrNull()
}

/**
 * Synchronous cache read for composables: a hit shows on the first frame
 * instead of flashing the source until the lookup coroutine runs.
 */
fun peekRuntimeTranslation(context: Context, source: String): String? {
    val cached =
        CachedTranslator(
            cacheDir = context.filesDir,
            // Peek never fetches; the lambda only satisfies the constructor.
            fetchFresh = { it },
        )
    return runCatching { cached.peek(source) }.getOrNull()
}

/**
 * Full resolution for one runtime string, shared by TranslatedText and
 * TranslatedMarkdown: synchronous cache hit first, then — only when a
 * screen-level prefetch already carries this string — a bounded wait for the
 * batch, and finally the individual request on a true miss.
 */
suspend fun resolveRuntimeTranslation(context: Context, source: String): String? {
    peekRuntimeTranslation(context, source)?.let { return it }
    if (!CachedTranslator.containsCjk(source)) return null
    val key = CachedTranslator.keyFor(source)
    if (TranslationPrefetch.isActive(key)) {
        kotlinx.coroutines.withTimeoutOrNull(PREFETCH_SETTLE_MS) {
            while (
                peekRuntimeTranslation(context, source) == null &&
                    TranslationPrefetch.isActive(key)
            ) {
                kotlinx.coroutines.delay(PREFETCH_POLL_MS)
            }
        }
        peekRuntimeTranslation(context, source)?.let { return it }
    }
    return translateRuntimeText(context, source)
}

internal const val PREFETCH_SETTLE_MS = 30_000L
private const val PREFETCH_POLL_MS = 300L

/**
 * Warms the translation cache for a whole screen in one or a few model
 * requests instead of one per string. Only CJK cache misses go over the
 * wire, in [PREFETCH_CHUNK_CHARS]-sized chunks; a failed chunk simply leaves
 * its strings uncached and they translate individually when composed.
 * Pure warm-up: it never changes what any composable displays.
 */
suspend fun prefetchRuntimeTranslations(context: Context, texts: List<String>) {
    val pending =
        texts
            .filter { CachedTranslator.containsCjk(it) }
            .distinct()
    if (pending.isEmpty()) return
    val service = EnhancedAIService.getInstance(context)
    val cached =
        CachedTranslator(
            cacheDir = context.filesDir,
            fetchFresh = { text -> service.translateText(text) },
        )
    val misses = pending.filter { cached.peek(it) == null }
    if (misses.isEmpty()) return
    TranslationPrefetch.mark(misses)
    try {
        for (chunk in chunkForPrefetch(misses)) {
            val fresh =
                runCatching { service.translateTexts(chunk) }.getOrNull()
                    ?: continue
            for ((index, source) in chunk.withIndex()) {
                fresh[index]?.let { cached.putTranslation(source, it) }
            }
        }
    } finally {
        TranslationPrefetch.unmark(misses)
    }
}

/**
 * Splits pending sources so no batch request exceeds [PREFETCH_CHUNK_CHARS]
 * characters of source text. One oversized source travels alone rather than
 * blocking the rest of the screen behind it.
 */
internal fun chunkForPrefetch(texts: List<String>): List<List<String>> {
    val chunks = mutableListOf<List<String>>()
    var current = mutableListOf<String>()
    var currentChars = 0
    for (text in texts) {
        if (current.isNotEmpty() && currentChars + text.length > PREFETCH_CHUNK_CHARS) {
            chunks.add(current)
            current = mutableListOf()
            currentChars = 0
        }
        current.add(text)
        currentChars += text.length
    }
    if (current.isNotEmpty()) chunks.add(current)
    return chunks
}

internal const val PREFETCH_CHUNK_CHARS = 6000

/** 工具结果详情弹窗 美观的弹窗显示完整的工具执行结果 */
@Composable
private fun ToolResultDetailDialog(
        toolName: String,
        result: String,
        isSuccess: Boolean,
        onDismiss: () -> Unit,
        onCopy: () -> Unit
) {
    val context = LocalContext.current
    val dialogMetrics = rememberCompactDialogMetrics()
    val resultMaxHeight = if (dialogMetrics.isCompactHeight) 160.dp else 300.dp
    // Tool results arrive as a single-line JSON envelope
    // ({"success":..,"message":..,"data":{...}}), which the dialog used to
    // dump verbatim. The envelope is still shown in full so no detail is
    // lost, but indented for reading, with the message line lifted above it.
    val (headline, body) = remember(result) { splitEnvelope(result) }
    // T1: the message line is runtime text — tool implementations write it
    // in Chinese. It goes through the cached LLM translation; the envelope
    // body underneath stays byte-identical for copy-paste. Untranslatable
    // (no model configured, offline) shows the source line, which is a
    // property of this feature, not a silent fallback: there is nothing
    // else this dialog could honestly show.
    //
    // The same applies to failure bodies: when the tool failed there is no
    // envelope, the body itself is the message (often a Chinese prefix from
    // the tool framework plus English detail), so it translates in place
    // while copy keeps the original bytes.
    val translatableBody = !isSuccess && headline == null
    var translatedHeadline by remember(headline) { mutableStateOf<String?>(null) }
    var translatedBody by remember(body, translatableBody) { mutableStateOf<String?>(null) }
    LaunchedEffect(headline, body) {
        translatedHeadline = null
        translatedBody = null
        val line = headline
        if (line != null) {
            if (!CachedTranslator.containsCjk(line)) return@LaunchedEffect
            translatedHeadline = translateRuntimeText(context, line)
        } else if (translatableBody && CachedTranslator.containsCjk(body)) {
            // A translated-away body must still differ from the source to be
            // worth swapping: identical output means the model echoed, and
            // the source stands.
            val translated = translateRuntimeText(context, body)
            translatedBody = translated?.takeIf { it.trim() != body.trim() }
        }
    }
    val cardModifier =
            Modifier.fillMaxWidth().padding(16.dp).compactDialogHeightWhenShort(dialogMetrics)
    Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
                modifier = cardModifier,
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 标题栏
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    // 状态图标
                    Icon(
                            imageVector =
                                    if (isSuccess) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = if (isSuccess) context.getString(R.string.success) else context.getString(R.string.failed),
                            tint =
                                    if (isSuccess) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // 工具名称
                    Text(
                            text = "$toolName ${if (isSuccess) context.getString(R.string.execution_success) else context.getString(R.string.execution_failed)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 复制按钮
                    IconButton(onClick = onCopy) {
                        Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = context.getString(R.string.copy_result),
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 分隔线
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.height(16.dp))

                // 结果内容
                (translatedHeadline ?: headline)?.let {
                    Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // Placeholder while the failure text translates; the success
                // path never waits because its body is data, not prose.
                if (translatableBody && translatedBody == null &&
                    CachedTranslator.containsCjk(body)
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .heightIn(min = 50.dp, max = resultMaxHeight)
                                        .verticalScroll(rememberScrollState())
                                        .background(
                                                color =
                                                        if (isSuccess)
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant.copy(
                                                                        alpha = 0.5f
                                                                )
                                                        else
                                                                MaterialTheme.colorScheme
                                                                        .errorContainer.copy(
                                                                        alpha = 0.2f
                                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp)
                ) {
                    Text(
                            text = translatedBody ?: body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 关闭按钮
                Button(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End),
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                )
                ) { Text(context.getString(R.string.close)) }
            }
        }
    }
}
