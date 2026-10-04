package com.ai.assistance.operit.ui.common.displays

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.ai.assistance.operit.data.translation.CachedTranslator
import com.ai.assistance.operit.ui.features.chat.components.part.translateRuntimeText

/**
 * Text that translates itself when it arrives in Chinese.
 *
 * Runtime strings — plugin names and descriptions, market listings — come
 * from outside the app resources, so English-only packaging cannot touch
 * them. When the text holds no CJK this is exactly Text(); otherwise one
 * cached LLM translation runs and the result sticks for every later frame.
 * Untranslatable shows the source.
 */
@Composable
fun TranslatedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    val context = LocalContext.current
    var translated by remember(text) { mutableStateOf<String?>(null) }
    LaunchedEffect(text) {
        translated = null
        if (CachedTranslator.containsCjk(text)) {
            translateRuntimeText(context, text)?.let { translated = it }
        }
    }
    Text(
        text = translated ?: text,
        modifier = modifier,
        color = color,
        style = style,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = onTextLayout,
    )
}
