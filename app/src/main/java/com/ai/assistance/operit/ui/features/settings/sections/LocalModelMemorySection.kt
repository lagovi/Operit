package com.ai.assistance.operit.ui.features.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.R
import com.ai.assistance.operit.api.speech.SpeechServiceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The way back from lazy keep-alive: the local checkpoint is loaded on the
 * first recognition and kept so later phrases start instantly, but it holds
 * hundreds of megabytes. This button releases it when the user is done with
 * speech recognition and wants the RAM back.
 *
 * The files stay on disk; the next recognition pays the load again. Only the
 * currently active local engine (whichever profile is selected) can be held
 * in memory at a time, so one button serves GigaAM, Parakeet and Nemotron.
 */
@Composable
fun LocalModelMemorySection(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(SpeechServiceFactory.isLocalServiceLoaded()) }
    var confirming by remember { mutableStateOf(false) }
    var result by remember {
        mutableStateOf<SpeechServiceFactory.LocalUnloadResult?>(null)
    }

    // A screen may have loaded (or unloaded) the engine since this tab was
    // first composed; re-read the live state whenever the section appears.
    LaunchedEffect(Unit) {
        loaded = withContext(Dispatchers.IO) { SpeechServiceFactory.isLocalServiceLoaded() }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.stt_memory_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.stt_memory_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { confirming = true },
            enabled = loaded,
        ) {
            Text(stringResource(R.string.stt_memory_unload))
        }
        result?.let { outcome ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    when (outcome) {
                        SpeechServiceFactory.LocalUnloadResult.UNLOADED ->
                            R.string.stt_memory_status_unloaded
                        SpeechServiceFactory.LocalUnloadResult.NOT_LOADED ->
                            R.string.stt_memory_status_not_loaded
                        SpeechServiceFactory.LocalUnloadResult.IN_USE ->
                            R.string.stt_memory_status_in_use
                        SpeechServiceFactory.LocalUnloadResult.FAILED ->
                            R.string.stt_memory_status_failed
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = when (outcome) {
                    SpeechServiceFactory.LocalUnloadResult.UNLOADED ->
                        MaterialTheme.colorScheme.onSurfaceVariant
                    SpeechServiceFactory.LocalUnloadResult.NOT_LOADED ->
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.error
                },
            )
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.stt_memory_confirm_title)) },
            text = { Text(stringResource(R.string.stt_memory_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        scope.launch(Dispatchers.IO) {
                            val outcome = SpeechServiceFactory.unloadLocalService()
                            withContext(Dispatchers.Main) {
                                result = outcome
                                loaded = SpeechServiceFactory.isLocalServiceLoaded()
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.stt_memory_action_unload))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) {
                    Text(stringResource(R.string.floating_cancel))
                }
            },
        )
    }
}
