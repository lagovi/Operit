package com.ai.assistance.operit.ui.features.llmio

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.model.LlmIoLogEntity
import com.ai.assistance.operit.ui.components.CustomScaffold
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val rowTimeFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM-dd HH:mm:ss", Locale.US)

/**
 * LLM I/O log: every formal-inference call with full request/response bodies,
 * a model filter, logging toggle, per-row detail with copy/share, and clear.
 * Entry point lives next to Token Usage Statistics in settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmIoLogScreen(
    onBackPressed: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: LlmIoLogViewModel = viewModel(factory = LlmIoLogViewModel.Factory(context))
    val state by viewModel.state.collectAsState()
    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    var detailEntry by remember { mutableStateOf<LlmIoLogEntity?>(null) }
    var filterExpanded by remember { mutableStateOf(false) }

    CustomScaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.llm_io_log_records, state.totalCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.llm_io_log_enabled),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = viewModel::setEnabled,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = filterExpanded,
                onExpandedChange = { filterExpanded = !filterExpanded },
            ) {
                TextField(
                    value = state.selectedModel
                        ?: stringResource(R.string.llm_io_log_all_models),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_model_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(filterExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = filterExpanded,
                    onDismissRequest = { filterExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.llm_io_log_all_models)) },
                        onClick = {
                            viewModel.selectModel(null)
                            filterExpanded = false
                        },
                    )
                    for (model in state.models) {
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                viewModel.selectModel(model)
                                filterExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                state.loading && state.entries.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.errorMessage != null && state.entries.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = viewModel::refresh) {
                                Text(stringResource(R.string.llm_io_log_retry))
                            }
                        }
                    }
                }
                state.entries.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.llm_io_log_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.entries, key = { it.id }) { entry ->
                            LlmIoLogRow(
                                entry = entry,
                                onOpen = { detailEntry = entry },
                            )
                        }
                        if (state.hasMore) {
                            item {
                                OutlinedButton(
                                    onClick = viewModel::loadMore,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(R.string.llm_io_log_load_more))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showClearConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.totalCount > 0,
            ) {
                Text(stringResource(R.string.llm_io_log_clear))
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.llm_io_log_clear_title)) },
            text = { Text(stringResource(R.string.llm_io_log_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.llm_io_log_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    detailEntry?.let { entry ->
        LlmIoLogDetailDialog(
            entry = entry,
            viewModel = viewModel,
            onDismiss = { detailEntry = null },
        )
    }
}

@Composable
private fun LlmIoLogRow(
    entry: LlmIoLogEntity,
    onOpen: () -> Unit,
) {
    val time =
        remember(entry.timestampMs) {
            Instant.ofEpochMilli(entry.timestampMs)
                .atZone(ZoneId.systemDefault())
                .format(rowTimeFormat)
        }
    val preview = remember(entry.responseText) { entry.responseText.lineSequence().take(2).joinToString("\n") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AssistChip(
                onClick = onOpen,
                label = { Text(entry.function) },
            )
            if (entry.error != null) {
                AssistChip(
                    onClick = onOpen,
                    label = { Text(stringResource(R.string.llm_io_log_error_badge)) },
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = entry.modelId,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (preview.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LlmIoLogDetailDialog(
    entry: LlmIoLogEntity,
    viewModel: LlmIoLogViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = entry.modelId,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                LlmIoLogMetaLine(
                    label = stringResource(R.string.settings_function_label),
                    value = entry.function,
                )
                LlmIoLogMetaLine(
                    label = stringResource(R.string.llm_io_log_latency),
                    value = entry.latencyMs?.let { "${it}ms" } ?: "?",
                )
                LlmIoLogMetaLine(
                    label = stringResource(R.string.llm_io_log_tokens),
                    value = "in=${entry.promptTokens ?: "?"} out=${entry.completionTokens ?: "?"}",
                )
                entry.error?.let {
                    LlmIoLogMetaLine(
                        label = stringResource(R.string.llm_io_log_error_badge),
                        value = it,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.llm_io_log_request_section),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = entry.requestJson,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.llm_io_log_response_section),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = entry.responseText.ifBlank { "-" },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(entry.requestJson))
                    }
                ) {
                    Text(stringResource(R.string.llm_io_log_copy_request))
                }
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(entry.responseText))
                    }
                ) {
                    Text(stringResource(R.string.llm_io_log_copy_response))
                }
                TextButton(
                    onClick = {
                        val shareIntent =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, viewModel.buildExportText(entry))
                            }
                        context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                context.getString(R.string.llm_io_log_share_title),
                            )
                        )
                    }
                ) {
                    Text(stringResource(R.string.share))
                }
            }
        },
    )
}

@Composable
private fun LlmIoLogMetaLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
