package com.ai.assistance.operit.ui.features.announcement

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.ui.common.displays.TranslatedText
import kotlinx.coroutines.delay

@Composable
fun RemoteAnnouncementDialog(
    title: String,
    body: String,
    acknowledgeText: String,
    onAcknowledge: () -> Unit,
    countdownSeconds: Int = 5
) {
    var remainingSeconds by remember(countdownSeconds) { mutableStateOf(countdownSeconds.coerceAtLeast(0)) }

    LaunchedEffect(countdownSeconds) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
    }

    val acknowledgeEnabled = remainingSeconds == 0

    val bodyScrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = {},
        // Upstream runtime text — translated like every other T1 surface.
        // The notice is prefetched before show (OperitApp), so the cache
        // hits on the first frame and no Chinese window appears.
        title = { TranslatedText(text = title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(bodyScrollState)
            ) {
                TranslatedText(text = body)
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge, enabled = acknowledgeEnabled) {
                // The static label translates once; the ticking countdown
                // stays a separate Text so no model request fires per second.
                Row {
                    TranslatedText(text = acknowledgeText)
                    if (!acknowledgeEnabled) {
                        Text(text = " (${remainingSeconds}s)")
                    }
                }
            }
        }
    )
}
