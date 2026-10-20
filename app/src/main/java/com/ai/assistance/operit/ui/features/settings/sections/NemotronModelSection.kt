package com.ai.assistance.operit.ui.features.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.ai.assistance.operit.data.speech.NemotronModelDownload
import com.ai.assistance.operit.data.speech.NemotronModelFiles
import com.ai.assistance.operit.data.speech.NemotronModelStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The one-time Nemotron download block, shown under the Nemotron engine in
 * settings. Same contract as [ParakeetModelSection]: declining the
 * dictation-screen consent stores no flag, and this block keeps offering the
 * download until the files exist.
 *
 * One extra versus the other engines: the sherpa-onnx JNI runtime is fetched
 * as an AAR and reduced to its arm64 library, so the first download also
 * carries the runtime once. [NemotronModelFiles.TOTAL_BYTES] counts the
 * weights; the runtime is a fixed extra the block mentions in its summary.
 */
@Composable
fun NemotronModelSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val downloads = remember(context) { NemotronModelDownload.getInstance(context) }
    val storage = remember(context) { NemotronModelStorage(context) }
    val state by downloads.state.collectAsState()

    LaunchedEffect(Unit) { downloads.refresh() }

    val sizeLabel = remember { formatMegabytes(NemotronModelFiles.TOTAL_BYTES) }
    // Keyed on a counter the picker bumps after every move: describe() reads
    // the live setting, but remember{} would otherwise pin the first answer
    // and the status line would keep showing the old location. Seen on device.
    var locationEpoch by remember { mutableStateOf(0) }
    val locationLabel = remember(locationEpoch) { storage.describe(storage.currentLocation()) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.nemotron_model_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(4.dp))

        val runtimeLabel = remember { formatMegabytes(NemotronModelFiles.RUNTIME_ASSET.sizeBytes) }
        val summary = when (val s = state) {
            is NemotronModelDownload.State.Ready ->
                stringResource(R.string.nemotron_ready_summary, sizeLabel, locationLabel)
            is NemotronModelDownload.State.Failed ->
                stringResource(R.string.nemotron_download_failed_summary, s.message)
            else ->
                stringResource(R.string.nemotron_absent_summary, sizeLabel, runtimeLabel)
        }
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = when (state) {
                is NemotronModelDownload.State.Failed -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

        if (state !is NemotronModelDownload.State.Ready) {
            locationLabel.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        StorageLocationPicker(
            storage = storage,
            downloads = downloads,
            sizeLabel = sizeLabel,
            onMoved = { locationEpoch++ },
        )
        Spacer(modifier = Modifier.height(8.dp))
        when (val s = state) {
            is NemotronModelDownload.State.Downloading -> {
                val fraction =
                    if (s.totalBytes > 0) s.downloadedBytes.toFloat() / s.totalBytes else 0f
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.stt_model_downloading,
                            (fraction * 100).toInt(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { downloads.cancel() }) {
                        Text(stringResource(R.string.floating_cancel))
                    }
                }
            }
            is NemotronModelDownload.State.Ready -> Unit
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { downloads.download() }) {
                        Text(stringResource(R.string.update_download))
                    }
                    if (state is NemotronModelDownload.State.Failed) {
                        OutlinedButton(onClick = { downloads.download() }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Where the model lives. The memory-card entry appears only while a removable
 * card is actually mounted; switching moves the downloaded files, and picking
 * first and downloading after works too, since an absent model has nothing to
 * move and only the setting flips.
 */
@Composable
private fun StorageLocationPicker(
    storage: NemotronModelStorage,
    downloads: NemotronModelDownload,
    sizeLabel: String,
    onMoved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    var moveFailed by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf(storage.currentLocation()) }
    var offered by remember { mutableStateOf(storage.availableLocations()) }

    fun refresh() {
        current = storage.currentLocation()
        offered = storage.availableLocations()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.stt_storage_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stringResource(R.string.stt_storage_summary, sizeLabel),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = storage.label(current),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Box {
                TextButton(
                    onClick = {
                        refresh()
                        expanded = true
                    },
                    enabled = !moving,
                ) {
                    Text(stringResource(R.string.stt_storage_change))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    offered.forEach { location ->
                        DropdownMenuItem(
                            text = { Text(storage.label(location)) },
                            onClick = {
                                expanded = false
                                if (location == current) return@DropdownMenuItem
                                moving = true
                                moveFailed = false
                                scope.launch(Dispatchers.IO) {
                                    val source = storage.currentLocation()
                                    val ok = storage.migrateTo(location)
                                    // Custom checkpoints live under the same
                                    // global root, so they travel with the
                                    // model instead of being left behind. The
                                    // source is captured up front because the
                                    // move above already flips the setting.
                                    val customsOk =
                                        com.ai.assistance.operit.data.speech.CustomSttModels(context)
                                            .migrateTo(source, location)
                                    withContext(Dispatchers.Main) {
                                        moving = false
                                        moveFailed = !ok || !customsOk
                                        refresh()
                                        // The files moved, so a stale Ready
                                        // pointing at the old dir must go.
                                        downloads.refresh()
                                        onMoved()
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
        if (moving) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.stt_storage_moving),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (moveFailed) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.stt_storage_move_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
