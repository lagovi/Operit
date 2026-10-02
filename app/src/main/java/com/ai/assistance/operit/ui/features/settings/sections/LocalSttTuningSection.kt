package com.ai.assistance.operit.ui.features.settings.sections

import android.media.MediaRecorder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.ai.assistance.operit.data.preferences.SpeechServicesPreferences
import kotlinx.coroutines.launch

/**
 * Tuning for the on-device engine. Every control writes through immediately;
 * the provider re-reads the setting at each session start, so changes apply
 * to the next recording without an app restart.
 */
@Composable
fun LocalSttTuningSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { SpeechServicesPreferences(context) }
    val tuning by prefs.localSttTuningFlow.collectAsState(
        initial = SpeechServicesPreferences.DEFAULT_LOCAL_STT_TUNING,
    )

    fun save(next: SpeechServicesPreferences.LocalSttTuning) {
        scope.launch { prefs.saveLocalSttTuning(next) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.stt_tuning_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.stt_tuning_vad),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(
                        if (tuning.vadEnabled) {
                            R.string.stt_tuning_vad_on_summary
                        } else {
                            R.string.stt_tuning_vad_off_summary
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = tuning.vadEnabled,
                onCheckedChange = { save(tuning.copy(vadEnabled = it)) },
            )
        }

        if (tuning.vadEnabled) {
            Spacer(modifier = Modifier.height(4.dp))
            TuningDropdown(
                label = stringResource(R.string.stt_tuning_sensitivity),
                value = stringResource(
                    if (tuning.vadAggressive) {
                        R.string.stt_tuning_sensitivity_aggressive
                    } else {
                        R.string.stt_tuning_sensitivity_normal
                    },
                ),
                options = listOf(
                    stringResource(R.string.stt_tuning_sensitivity_normal) to false,
                    stringResource(R.string.stt_tuning_sensitivity_aggressive) to true,
                ),
                onSelect = { save(tuning.copy(vadAggressive = it)) },
            )
            if (tuning.vadAggressive) {
                Text(
                    text = stringResource(R.string.stt_tuning_sensitivity_aggressive_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            TuningDropdown(
                label = stringResource(R.string.stt_tuning_endpoint),
                value = stringResource(
                    R.string.stt_tuning_endpoint_seconds,
                    tuning.endpointSilenceMs / 1000f,
                ),
                options = SpeechServicesPreferences.LocalSttTuning.ENDPOINT_OPTIONS_MS.map { ms ->
                    stringResource(R.string.stt_tuning_endpoint_seconds, ms / 1000f) to ms
                },
                onSelect = { save(tuning.copy(endpointSilenceMs = it)) },
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        TuningDropdown(
            label = stringResource(R.string.stt_tuning_mic),
            value = stringResource(
                if (tuning.micSource == MediaRecorder.AudioSource.VOICE_RECOGNITION) {
                    R.string.stt_tuning_mic_recognition
                } else {
                    R.string.stt_tuning_mic_communication
                },
            ),
            options = listOf(
                stringResource(R.string.stt_tuning_mic_communication) to
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                stringResource(R.string.stt_tuning_mic_recognition) to
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
            ),
            onSelect = { save(tuning.copy(micSource = it)) },
        )
    }
}

@Composable
private fun <T> TuningDropdown(
    label: String,
    value: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(value)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (text, payload) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            expanded = false
                            onSelect(payload)
                        },
                    )
                }
            }
        }
    }
}
}
