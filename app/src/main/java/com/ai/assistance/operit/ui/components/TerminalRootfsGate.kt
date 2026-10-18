package com.ai.assistance.operit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.R
import com.ai.assistance.operit.util.TerminalRootfsDownload

/**
 * Gates a terminal screen behind the one-time Ubuntu rootfs download.
 *
 * The archive is no longer inside the APK (English-only fork, HANDOFF N69), so
 * the terminal cannot start until it is on the device. This shows the consent,
 * the progress and the retry around that transfer, and only composes [content]
 * once the terminal can actually start. The same transfer backs
 * [TerminalRootfsDownload.ensureArchive], used when the terminal is started
 * without a UI.
 */
@Composable
fun TerminalRootfsGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val manager = remember { TerminalRootfsDownload.getInstance(context) }
    val state by manager.state.collectAsState()

    LaunchedEffect(Unit) { manager.refresh() }

    if (state is TerminalRootfsDownload.State.Ready) {
        content()
        return
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (val s = state) {
                    is TerminalRootfsDownload.State.Unknown,
                    is TerminalRootfsDownload.State.Checking -> {
                        CircularProgressIndicator()
                    }

                    is TerminalRootfsDownload.State.Absent -> {
                        Text(
                            stringResource(R.string.terminal_rootfs_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.terminal_rootfs_required_body),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = { manager.download() }) {
                            Text(stringResource(R.string.download))
                        }
                    }

                    is TerminalRootfsDownload.State.Downloading -> {
                        Text(
                            stringResource(R.string.terminal_rootfs_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(16.dp))
                        val fraction =
                            if (s.totalBytes > 0L) {
                                (s.downloadedBytes.toFloat() / s.totalBytes.toFloat())
                                    .coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.terminal_rootfs_downloading),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { manager.cancel() }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }

                    is TerminalRootfsDownload.State.Failed -> {
                        Text(
                            stringResource(R.string.terminal_rootfs_failed_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            s.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = { manager.download() }) {
                            Text(stringResource(R.string.terminal_rootfs_retry))
                        }
                    }

                    is TerminalRootfsDownload.State.Ready -> Unit
                }
            }
        }
    }
}
