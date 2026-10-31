package com.ai.assistance.operit.ui.features.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.R
import com.ai.assistance.operit.ui.components.CustomScaffold
import com.ai.assistance.operit.util.PaddleOcrEngine
import kotlinx.coroutines.launch

@Composable
fun ModuleCenterScreen(
    onNavigateToSpeechServices: () -> Unit,
    onNavigateToMnnModelDownload: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ocrPrepared by remember { mutableStateOf(PaddleOcrEngine.isPrepared(context)) }
    var ocrBusy by remember { mutableStateOf(false) }
    var ocrError by remember { mutableStateOf<String?>(null) }

    CustomScaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.module_center_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                ModuleCard(
                    icon = Icons.Default.RecordVoiceOver,
                    title = stringResource(R.string.module_center_speech_title),
                    description = stringResource(R.string.module_center_speech_desc),
                    status = null,
                    actionLabel = stringResource(R.string.module_center_open),
                    onAction = onNavigateToSpeechServices,
                )
            }

            item {
                ModuleCard(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.module_center_ocr_title),
                    description = stringResource(R.string.module_center_ocr_desc),
                    status =
                        if (ocrPrepared) stringResource(R.string.module_center_ocr_ready)
                        else stringResource(R.string.module_center_ocr_missing),
                    actionLabel =
                        if (ocrPrepared) stringResource(R.string.module_center_ocr_ready)
                        else stringResource(R.string.module_center_prepare),
                    actionEnabled = !ocrPrepared,
                    actionBusy = ocrBusy,
                    onAction = {
                        scope.launch {
                            ocrBusy = true
                            ocrError = null
                            val result = PaddleOcrEngine.prepare(context)
                            ocrPrepared = result.isSuccess
                            ocrError = result.exceptionOrNull()?.message
                            ocrBusy = false
                        }
                    },
                )
            }

            ocrError?.let { message ->
                item {
                    Text(
                        text = stringResource(R.string.module_center_ocr_failed, message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            item {
                ModuleCard(
                    icon = Icons.Default.Memory,
                    title = stringResource(R.string.module_center_mnn_title),
                    description = stringResource(R.string.module_center_mnn_desc),
                    status = null,
                    actionLabel = stringResource(R.string.module_center_open),
                    onAction = onNavigateToMnnModelDownload,
                )
            }
        }
    }
}

@Composable
private fun ModuleCard(
    icon: ImageVector,
    title: String,
    description: String,
    status: String?,
    actionLabel: String,
    actionEnabled: Boolean = true,
    actionBusy: Boolean = false,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                status?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            if (it == stringResource(R.string.module_center_ocr_ready)) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (actionBusy) {
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.module_center_preparing))
                }
            } else {
                Button(
                    onClick = onAction,
                    enabled = actionEnabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
