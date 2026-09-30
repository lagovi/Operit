package com.ai.assistance.operit.ui.features.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.R
import com.ai.assistance.operit.util.LocaleUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Language settings.
 *
 * Fork: this build ships one locale, so the screen is informational rather than
 * a picker. The list is driven by [LocaleUtils.getSupportedLanguages] — adding a
 * language means adding a resource directory and one entry there, and this
 * screen becomes a picker with no other change. The selection path is kept
 * working so that entry is the only thing that has to be correct.
 */
@Composable
fun LanguageSettingsScreen(onBackPressed: () -> Unit) {
    val supportedLanguages = LocaleUtils.getSupportedLanguages()
    var currentLanguage by remember { mutableStateOf(supportedLanguages.first().code) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { currentLanguage = LocaleUtils.currentLanguageCode() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.language_info),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(supportedLanguages) { language ->
                val isSelected = language.code == currentLanguage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            onClick = {
                                if (language.code == currentLanguage) return@selectable
                                val context = LocalContext.current
                                currentLanguage = language.code
                                scope.launch {
                                    delay(300)
                                    LocaleUtils.setAppLanguage(context, language.code)
                                }
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = language.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        if (language.nativeName != language.displayName) {
                            Text(
                                text = language.nativeName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.confirm_action),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
