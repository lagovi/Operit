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
import com.ai.assistance.operit.data.speech.GigaAMModelDownload
import com.ai.assistance.operit.data.speech.GigaAMModelFiles
import com.ai.assistance.operit.data.speech.SttModelStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The one-time GigaAM download block, shown under the local engine in
 * settings. This is also the way back after declining the dictation-screen
 * consent: declining stores no flag, the files simply stay absent, and this
 * block keeps offering the download until they exist.
 */
@Composable
fun GigaAMModelSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val downloads = remember(context) { GigaAMModelDownload.getInstance(context) }
    val storage = remember(context) { SttModelStorage(context) }
    val state by downloads.state.collectAsState()

    LaunchedEffect(Unit) { downloads.refresh() }

    val sizeLabel = remember { formatMegabytes(GigaAMModelFiles.TOTAL_BYTES) }
    // Keyed on a counter the picker bumps after every move: describe() reads
    // the live setting, but remember{} would otherwise pin the first answer
    // and the status line would keep showing the old location. Seen on device.
    var locationEpoch by remember { mutableStateOf(0) }
    val locationLabel = remember(locationEpoch) { storage.describe(storage.currentLocation()) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.gigaam_model_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(4.dp))

        val summary = when (val s = state) {
            is GigaAMModelDownload.State.Ready ->
                stringResource(R.string.gigaam_ready_summary, sizeLabel, locationLabel)
            is GigaAMModelDownload.State.Failed ->
                stringResource(R.string.gigaam_download_failed_summary, s.message)
            else ->
                stringResource(R.string.gigaam_absent_summary, sizeLabel)
        }
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = when (state) {
                is GigaAMModelDownload.State.Failed -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

        if (state !is GigaAMModelDownload.State.Ready) {
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
        StorageLocationPicker(storage = storage, onMoved = { locationEpoch++ })
        CustomSttModelSection(locationEpoch = locationEpoch)
        Spacer(modifier = Modifier.height(8.dp))
        when (val s = state) {
            is GigaAMModelDownload.State.Downloading -> {
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
            is GigaAMModelDownload.State.Ready -> Unit
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { downloads.download() }) {
                        Text(stringResource(R.string.update_download))
                    }
                    if (state is GigaAMModelDownload.State.Failed) {
                        OutlinedButton(onClick = { downloads.download() }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }
        }
    }
}

/** 224762512 bytes reads as "225 MB", which is what the consent screen quotes. */
internal fun formatMegabytes(bytes: Long): String = "${(bytes + 500_000L) / 1_000_000L} MB"

/**
 * Where the model lives. The memory-card entry appears only while a removable
 * card is actually mounted; switching moves the downloaded files, and picking
 * first and downloading after works too, since an absent model has nothing to
 * move and only the setting flips.
 */
@Composable
private fun StorageLocationPicker(storage: SttModelStorage, onMoved: () -> Unit) {
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
            text = stringResource(R.string.stt_storage_summary),
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
                                    // root, so they travel with the built-in
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
                                        GigaAMModelDownload.getInstance(context).refresh()
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
