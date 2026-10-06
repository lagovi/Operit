package com.ai.assistance.operit.services.core

import android.content.Context
import com.ai.assistance.operit.api.chat.enhance.MultiServiceManager
import com.ai.assistance.operit.core.chat.hooks.PromptTurn
import com.ai.assistance.operit.core.chat.hooks.PromptTurnKind
import com.ai.assistance.operit.core.config.FunctionalPrompts
import com.ai.assistance.operit.data.model.FunctionType
import com.ai.assistance.operit.data.model.LlmIoLogEntity
import com.ai.assistance.operit.data.preferences.FunctionalConfigManager
import com.ai.assistance.operit.data.preferences.SupervisionPreferences
import com.ai.assistance.operit.data.stats.LlmIoLogRepository
import com.ai.assistance.operit.data.stats.ProviderUsageSnapshot
import com.ai.assistance.operit.data.translation.stripThinkBlocks
import com.ai.assistance.operit.util.AppLogger
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Supervision observer: after a driver turn completes, a smart model reads a
 * compact digest (user text, invoked tool names, final answer) and answers
 * with strict JSON {comment, corrected_call}. The comment goes to the chat
 * toast queue, the parsed verdict goes to the LLM I/O log with
 * function=SUPERVISION.
 *
 * No-recursion property (structural, not a flag): the observer call goes
 * directly through [com.ai.assistance.operit.api.chat.llmprovider.AIService.sendMessage]
 * on a leased functional service. It never enters EnhancedAIService or the
 * turn loop, so the observer cannot observe itself. The observer call also
 * uses recordTokenUsage=false, so it stays out of token statistics and out
 * of the automatic I/O-log hook; this class writes the single enriched row
 * itself instead.
 *
 * Observer failures are silent by contract (no comment), but always leave a
 * log row with a short error reason for debugging.
 */
class SupervisionObserver(
    context: Context,
    private val scope: CoroutineScope,
    private val showToast: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val serviceManager = MultiServiceManager(appContext)
    private val functionalConfigManager = FunctionalConfigManager(appContext)
    private val preferences = SupervisionPreferences(appContext)
    private val logRepository = LlmIoLogRepository.getInstance(appContext)

    /** Turn keys already observed; the turn-end hook must fire exactly once. */
    private val observedTurns = ConcurrentHashMap<String, Boolean>()

    /** Last observer toast per chat, for the cooldown. */
    private val lastToastAtByChat = ConcurrentHashMap<String, Long>()

    /** Last seen SUPERVISION binding; on change the cached service is retired. */
    @Volatile
    private var lastMapping: String? = null

    fun observeTurn(
        chatId: String,
        turnId: Long,
        userText: String,
        toolNames: List<String>,
        finalAnswer: String,
        driverProvider: String,
        driverModel: String,
    ) {
        if (!preferences.isEnabled()) return
        if (userText.isBlank()) return
        val key = "$chatId:$turnId"
        if (observedTurns.putIfAbsent(key, true) != null) return
        if (observedTurns.size > MAX_TRACKED_TURNS) {
            observedTurns.clear()
        }
        scope.launch(Dispatchers.IO) {
            runObservation(chatId, userText, toolNames, finalAnswer, driverProvider, driverModel)
        }
    }

    private suspend fun runObservation(
        chatId: String,
        userText: String,
        toolNames: List<String>,
        finalAnswer: String,
        driverProvider: String,
        driverModel: String,
    ) {
        val driverModelId = "$driverProvider:$driverModel"
        val digestJson = buildDigestJson(driverModelId, userText, toolNames, finalAnswer)
        val startedAtMs = System.currentTimeMillis()
        var rawReply = ""
        var usage: ProviderUsageSnapshot? = null
        var leaseModelId = "unknown"
        var leaseConfigId = ""
        var parameters: List<com.ai.assistance.operit.data.model.ModelParameter<*>> = emptyList()
        try {
            refreshServiceOnMappingChange()
            val lease = serviceManager.acquireServiceForFunction(FunctionType.SUPERVISION)
            try {
                leaseModelId = lease.service.providerModel
                leaseConfigId = lease.modelConfig.id
                parameters = serviceManager.getModelParametersForFunction(FunctionType.SUPERVISION)
                val turns =
                    listOf(
                        PromptTurn(
                            kind = PromptTurnKind.SYSTEM,
                            content = FunctionalPrompts.SUPERVISION_SYSTEM_PROMPT.trimIndent(),
                        ),
                        PromptTurn(
                            kind = PromptTurnKind.USER,
                            content = FunctionalPrompts.supervisionUserPrompt(digestJson),
                        ),
                    )
                val buffer = StringBuilder()
                lease.service
                    .sendMessage(
                        context = appContext,
                        chatHistory = turns,
                        modelParameters = parameters,
                        enableThinking = false,
                        stream = false,
                        availableTools = null,
                        recordTokenUsage = false,
                        enableRetry = false,
                        onUsageReported = { snapshot, _ ->
                            usage = snapshot
                        },
                    )
                    .collect { chunk -> buffer.append(chunk) }
                rawReply = buffer.toString()
            } finally {
                lease.close()
            }
            val latencyMs = System.currentTimeMillis() - startedAtMs
            val verdict = parseVerdict(rawReply)
            if (verdict.comment.isNotBlank()) {
                postCommentWithCooldown(chatId, verdict.comment)
            }
            logRepository.record(
                LlmIoLogEntity(
                    timestampMs = startedAtMs,
                    function = LlmIoLogRepository.FUNCTION_SUPERVISION,
                    modelId = leaseModelId,
                    configId = leaseConfigId,
                    requestJson =
                        LlmIoLogRepository.formatRequest(
                            turns =
                                listOf(
                                    PromptTurn(
                                        kind = PromptTurnKind.SYSTEM,
                                        content = FunctionalPrompts.SUPERVISION_SYSTEM_PROMPT.trimIndent(),
                                    ),
                                    PromptTurn(
                                        kind = PromptTurnKind.USER,
                                        content =
                                            FunctionalPrompts.supervisionUserPrompt(digestJson),
                                    ),
                                ),
                            parameters = parameters,
                            enableThinking = false,
                            stream = false,
                        ),
                    responseText =
                        LlmIoLogRepository.truncateMiddle(
                            rawReply,
                            LlmIoLogRepository.MAX_RESPONSE_CHARS,
                        ),
                    promptTokens =
                        usage?.let {
                            LlmIoLogRepository.promptTokensOf(
                                it.uncachedInputTokens,
                                it.cachedInputTokens,
                                it.cacheWriteTokens,
                                it.totalInputTokens,
                            )
                        },
                    completionTokens = usage?.outputTokens,
                    latencyMs = latencyMs,
                    error = if (verdict.verdict == VERDICT_PARSE_ERROR) ERROR_PARSE else null,
                    supervisionComment = verdict.comment.ifBlank { null },
                    supervisionCorrectedCall = verdict.correctedCallJson,
                    supervisionVerdict = verdict.verdict,
                    supervisionDigest = digestJson,
                    supervisionDriverModel = driverModelId,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "supervision observation failed", e)
            runCatching {
                logRepository.record(
                    LlmIoLogEntity(
                        timestampMs = startedAtMs,
                        function = LlmIoLogRepository.FUNCTION_SUPERVISION,
                        modelId = leaseModelId,
                        configId = leaseConfigId,
                        requestJson =
                            LlmIoLogRepository.formatRequest(
                                turns =
                                    listOf(
                                        PromptTurn(
                                            kind = PromptTurnKind.USER,
                                            content = digestJson,
                                        ),
                                    ),
                                parameters = parameters,
                                enableThinking = false,
                                stream = false,
                            ),
                        responseText =
                            LlmIoLogRepository.truncateMiddle(
                                rawReply,
                                LlmIoLogRepository.MAX_RESPONSE_CHARS,
                            ),
                        latencyMs = System.currentTimeMillis() - startedAtMs,
                        error = (e.message?.take(ERROR_MAX_CHARS) ?: e.javaClass.simpleName),
                        supervisionVerdict = VERDICT_OBSERVER_ERROR,
                        supervisionDigest = digestJson,
                        supervisionDriverModel = driverModelId,
                    ),
                )
            }
        }
    }

    /**
     * The observer manager is separate from the chat stack manager, so config
     * changes made in FunctionalConfigScreen do not refresh it. Re-check the
     * SUPERVISION binding on every observation and retire the cached service
     * when it moved.
     */
    private suspend fun refreshServiceOnMappingChange() {
        val mapping =
            functionalConfigManager.getConfigMappingForFunction(FunctionType.SUPERVISION)
        val key = "${mapping.configId}#${mapping.modelIndex}"
        if (lastMapping == null) {
            lastMapping = key
            return
        }
        if (lastMapping != key) {
            lastMapping = key
            serviceManager.refreshServiceForFunction(FunctionType.SUPERVISION)
        }
    }

    private fun postCommentWithCooldown(chatId: String, comment: String) {
        val now = System.currentTimeMillis()
        val last = lastToastAtByChat[chatId] ?: 0L
        if (now - last < COOLDOWN_MS) return
        lastToastAtByChat[chatId] = now
        showToast(LlmIoLogRepository.truncateMiddle(comment, MAX_COMMENT_CHARS))
    }

    private fun buildDigestJson(
        driverModelId: String,
        userText: String,
        toolNames: List<String>,
        finalAnswer: String,
    ): String {
        val digest = JSONObject()
        digest.put("driver_model", driverModelId)
        digest.put(
            "user",
            LlmIoLogRepository.truncateMiddle(userText.trim(), MAX_DIGEST_USER_CHARS),
        )
        val tools = JSONArray()
        toolNames.take(MAX_TOOL_NAMES).forEach { tools.put(it) }
        digest.put("tools", tools)
        digest.put("tool_count", toolNames.size)
        digest.put(
            "final",
            LlmIoLogRepository.truncateMiddle(finalAnswer.trim(), MAX_DIGEST_FINAL_CHARS),
        )
        return digest.toString()
    }

    private data class Verdict(
        val comment: String,
        val correctedCallJson: String?,
        val verdict: String,
    )

    private fun parseVerdict(rawReply: String): Verdict {
        // Reasoning models wrap their work in <think> tags (same as the
        // translation pipeline); the JSON contract lives outside them.
        val stripped = stripJsonFences(stripThinkBlocks(rawReply)).trim()
        if (stripped.isEmpty()) {
            return Verdict("", null, VERDICT_PARSE_ERROR)
        }
        return try {
            val json = JSONObject(stripped)
            val comment = json.optString("comment", "")
            val corrected =
                if (json.isNull("corrected_call")) {
                    null
                } else {
                    json.optJSONObject("corrected_call")?.toString()
                }
            val verdict = if (corrected != null) VERDICT_CORRECTED else VERDICT_OK
            Verdict(comment, corrected, verdict)
        } catch (e: Exception) {
            AppLogger.w(TAG, "supervision reply is not strict JSON")
            Verdict("", null, VERDICT_PARSE_ERROR)
        }
    }

    private fun stripJsonFences(text: String): String {
        var stripped = text.trim()
        if (!stripped.startsWith("```")) return stripped
        stripped = stripped.removePrefix("```")
        val firstNewline = stripped.indexOf('\n')
        if (firstNewline >= 0 && stripped.substring(0, firstNewline).trim().length <= 8) {
            stripped = stripped.substring(firstNewline + 1)
        }
        val fence = stripped.lastIndexOf("```")
        if (fence >= 0) {
            stripped = stripped.substring(0, fence)
        }
        return stripped.trim()
    }

    companion object {
        private const val TAG = "SupervisionObserver"
        const val VERDICT_OK = "ok"
        const val VERDICT_CORRECTED = "corrected"
        const val VERDICT_PARSE_ERROR = "parse_error"
        const val VERDICT_OBSERVER_ERROR = "observer_error"
        const val ERROR_PARSE = "supervision_parse_error"
        const val MAX_COMMENT_CHARS = 500
        const val MAX_DIGEST_USER_CHARS = 2_000
        const val MAX_DIGEST_FINAL_CHARS = 4_000
        const val MAX_TOOL_NAMES = 50
        const val ERROR_MAX_CHARS = 200
        const val COOLDOWN_MS = 30_000L
        const val MAX_TRACKED_TURNS = 500
    }
}
