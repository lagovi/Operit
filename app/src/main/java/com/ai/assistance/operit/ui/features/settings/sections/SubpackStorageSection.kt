package com.ai.assistance.operit.ui.features.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.ai.assistance.operit.core.subpack.SubpackStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Where the APK/EXE export templates live. Same move mechanics as the
 * speech model picker, without the speech specifics: the staged ~35 MB
 * can sit on the memory card instead of internal storage.
 */
@Composable
fun SubpackStorageSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val storage = remember { SubpackStorage(context) }
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

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.subpack_storage_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stringResource(R.string.subpack_storage_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = storage.describe(current).ifBlank { storage.label(current) },
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
                    Text(stringResource(R.string.subpack_storage_change))
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
                                    val ok = storage.migrateTo(location)
                                    withContext(Dispatchers.Main) {
                                        moving = false
                                        moveFailed = !ok
                                        refresh()
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
                text = stringResource(R.string.subpack_storage_moving),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (moveFailed) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.subpack_storage_move_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
