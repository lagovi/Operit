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
import com.ai.assistance.operit.ui.features.chat.components.part.peekRuntimeTranslation
import com.ai.assistance.operit.ui.features.chat.components.part.resolveRuntimeTranslation

/**
 * Text that translates itself when it arrives in Chinese.
 *
 * Runtime strings — plugin names and descriptions, market listings — come
 * from outside the app resources, so English-only packaging cannot touch
 * them. When the text holds no CJK this is exactly Text(). A cached
 * translation shows on the first frame; otherwise one resolution runs
 * (batch prefetch first when a screen warmed it, individual request
 * otherwise) and the result sticks for every later frame. Untranslatable
 * shows the source.
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
    // Synchronous cache read: a warmed screen never flashes the source.
    var translated by remember(text) { mutableStateOf(peekRuntimeTranslation(context, text)) }
    LaunchedEffect(text) {
        if (translated == null) {
            translated = resolveRuntimeTranslation(context, text)
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
