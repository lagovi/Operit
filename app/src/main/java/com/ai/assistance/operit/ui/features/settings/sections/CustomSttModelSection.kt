package com.ai.assistance.operit.ui.features.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.ai.assistance.operit.data.speech.CustomSttModel
import com.ai.assistance.operit.data.speech.CustomSttModels
import com.ai.assistance.operit.data.speech.HfCtcCandidate
import com.ai.assistance.operit.data.speech.HfModelResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The checkpoint picker: the built-in model plus every user-added one.
 * Selecting takes effect on the next dictation session; the provider keeps
 * its already-open recognizer for the running one.
 *
 * @param locationEpoch bumped by the storage picker after every move, so the
 *   per-model status lines re-read the new location instead of pinning the
 *   old one. Same staleness trap as the built-in block above.
 */
@Composable
fun CustomSttModelSection(locationEpoch: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val models = remember(context) { CustomSttModels(context) }
    val prefs = remember(context) { SpeechServicesPreferences(context) }
    val scope = rememberCoroutineScope()

    var entries by remember { mutableStateOf<List<CustomSttModel>>(emptyList()) }
    var downloaded by remember { mutableStateOf(setOf<String>()) }
    val selectedId by prefs.localSttModelIdFlow.collectAsState(
        initial = SpeechServicesPreferences.BUILTIN_GIGAAM_ID,
    )
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CustomSttModel?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        entries = models.list()
        downloaded = entries
            .filter { models.downloadedDir(it) != null }
            .map { it.id }
            .toSet()
    }
    LaunchedEffect(locationEpoch) { refresh() }

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.stt_custom_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stringResource(R.string.stt_custom_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))

        CheckpointRow(
            name = stringResource(R.string.stt_custom_builtin_name),
            status = null,
            selected = selectedId == SpeechServicesPreferences.BUILTIN_GIGAAM_ID,
            onSelect = {
                scope.launch(Dispatchers.IO) {
                    prefs.saveLocalSttModelId(SpeechServicesPreferences.BUILTIN_GIGAAM_ID)
                }
            },
            onDelete = null,
        )
        entries.forEach { entry ->
            val ready = downloaded.contains(entry.id)
            CheckpointRow(
                name = entry.displayName,
                status = if (ready) {
                    stringResource(
                        R.string.stt_custom_ready,
                        formatMegabytes(entry.modelSizeBytes),
                    )
                } else {
                    stringResource(R.string.stt_custom_not_downloaded)
                },
                selected = selectedId == entry.id,
                onSelect = {
                    scope.launch(Dispatchers.IO) {
                        prefs.saveLocalSttModelId(entry.id)
                    }
                },
                onDelete = { pendingDelete = entry },
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(onClick = { showAdd = true }) {
            Text(stringResource(R.string.stt_custom_add))
        }
        notice?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    if (showAdd) {
        AddFromLinkDialog(
            onDismiss = { showAdd = false },
            onAdded = { entry ->
                showAdd = false
                scope.launch(Dispatchers.IO) {
                    prefs.saveLocalSttModelId(entry.id)
                    withContext(Dispatchers.Main) { refresh() }
                }
            },
            onFailed = { notice = it },
        )
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch(Dispatchers.IO) {
                        models.remove(entry.id)
                        val fellBack = selectedId == entry.id
                        if (fellBack) {
                            prefs.saveLocalSttModelId(SpeechServicesPreferences.BUILTIN_GIGAAM_ID)
                        }
                        withContext(Dispatchers.Main) {
                            refresh()
                            if (fellBack) {
                                notice = context.getString(
                                    R.string.stt_custom_deleted_builtin_fallback,
                                )
                            }
                        }
                    }
                }) {
                    Text(stringResource(R.string.stt_custom_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.floating_cancel))
                }
            },
            text = {
                Text(stringResource(R.string.stt_custom_delete_confirm, entry.displayName))
            },
        )
    }
}

@Composable
private fun CheckpointRow(
    name: String,
    status: String?,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            val line = when {
                selected && status != null ->
                    "${stringResource(R.string.stt_custom_selected)} · $status"
                selected -> stringResource(R.string.stt_custom_selected)
                status != null -> status
                else -> null
            }
            line?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onDelete != null) {
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.stt_custom_delete))
            }
        }
    }
}

/**
 * Two-step add: look the link up first so the user confirms the exact
 * checkpoint and size before any big byte moves, then the add pipeline
 * (tokens pin, download, graph-vocabulary handshake) runs.
 */
@Composable
private fun AddFromLinkDialog(
    onDismiss: () -> Unit,
    onAdded: (CustomSttModel) -> Unit,
    onFailed: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var candidate by remember { mutableStateOf<HfCtcCandidate?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        confirmButton = {
            val found = candidate
            TextButton(
                onClick = {
                    if (found == null) {
                        busy = true
                        error = null
                        scope.launch(Dispatchers.IO) {
                            val result = HfModelResolver().resolve(link)
                            withContext(Dispatchers.Main) {
                                busy = false
                                result
                                    .onSuccess { candidate = it }
                                    .onFailure {
                                        error = it.message ?: it.toString()
                                    }
                            }
                        }
                    } else {
                        busy = true
                        error = null
                        scope.launch(Dispatchers.IO) {
                            val result = CustomSttModels(context).addFromCandidate(found)
                            withContext(Dispatchers.Main) {
                                busy = false
                                result
                                    .onSuccess { onAdded(it) }
                                    .onFailure {
                                        val message = context.getString(
                                            R.string.stt_custom_add_failed,
                                            it.message ?: it.toString(),
                                        )
                                        error = message
                                        onFailed(message)
                                    }
                            }
                        }
                    }
                },
                enabled = !busy && (found != null || link.isNotBlank()),
            ) {
                Text(
                    stringResource(
                        if (found != null) R.string.stt_custom_add else R.string.stt_custom_resolve,
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!busy) onDismiss() }) {
                Text(stringResource(R.string.floating_cancel))
            }
        },
        title = { Text(stringResource(R.string.stt_custom_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = link,
                    onValueChange = {
                        link = it
                        candidate = null
                        error = null
                    },
                    enabled = !busy,
                    label = { Text(stringResource(R.string.stt_custom_link_hint)) },
                    supportingText = { Text(stringResource(R.string.stt_custom_link_example)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(
                            if (candidate != null) {
                                R.string.stt_custom_adding
                            } else {
                                R.string.stt_custom_resolving
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                candidate?.let {
                    Text(
                        text = stringResource(
                            R.string.stt_custom_confirm_add,
                            "${it.repoId}@${it.revision}/${it.modelFile.path}",
                            formatMegabytes(it.modelFile.sizeBytes),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
}
