package com.ai.assistance.operit.ui.common.displays

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.ai.assistance.operit.data.translation.CachedTranslator
import com.ai.assistance.operit.ui.features.chat.components.part.translateRuntimeText

/**
 * Markdown that translates itself when it arrives in Chinese.
 *
 * Same T1 pattern as TranslatedText, but the (possibly translated) source
 * keeps going through the markdown renderer so formatting survives.
 * Untranslatable shows the source. Code blocks must not come here: the
 * translation would corrupt commands and config keys.
 */
@Composable
fun TranslatedMarkdown(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var translated by remember(text) { mutableStateOf<String?>(null) }
    LaunchedEffect(text) {
        translated = null
        if (CachedTranslator.containsCjk(text)) {
            translateRuntimeText(context, text)?.let { translated = it }
        }
    }
    MarkdownTextComposable(
        text = translated ?: text,
        textColor = textColor,
        modifier = modifier,
    )
}
