package com.ai.assistance.operit.ui.features.help

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ai.assistance.operit.ui.features.help.content.HelpTopic
import com.ai.assistance.operit.ui.features.help.content.helpEntry
import com.ai.assistance.operit.ui.features.help.content.helpTopic

/** Element-level help shown while the screen is in help mode. */
data class HelpEntry(
    val id: String,
    val title: String,
    val body: String,
)

/**
 * State for the in-place help mode.
 *
 * The mode is deliberately UI-local: it never changes the screen content and
 * it never blocks navigation. While it is on, [Modifier.helpTapInterceptor]
 * turns short taps into element descriptions, and [Modifier.helpAnchor]
 * publishes the hit rectangles used to decide which control was tapped.
 */
class HelpModeState {
    var enabled by mutableStateOf(false)
        private set

    var screenId by mutableStateOf("")
        private set

    var activeEntry by mutableStateOf<HelpEntry?>(null)
        private set

    var activeTopic by mutableStateOf<HelpTopic?>(null)
        private set

    private val anchors = mutableMapOf<String, Rect>()
    private var layerOrigin = Offset.Zero

    fun setLayerOrigin(origin: Offset) {
        layerOrigin = origin
    }

    fun register(scope: String, id: String, rect: Rect) {
        anchors["$scope:$id"] = rect
    }

    fun unregister(scope: String, id: String) {
        anchors.remove("$scope:$id")
    }

    fun tap(position: Offset) {
        val absolute = position + layerOrigin
        val hit =
            anchors.entries
                .asSequence()
                .filter { it.key.startsWith("$screenId:") && it.value.contains(absolute) }
                .minByOrNull { it.value.width * it.value.height }
        val elementId = hit?.key?.substringAfter(':')
        if (elementId != null) {
            helpEntry(screenId, elementId)?.let {
                activeEntry = it
                return
            }
        }
        activeTopic = helpTopic(screenId)
    }

    fun bindScreen(id: String?) {
        if (screenId != id.orEmpty()) {
            enabled = false
            activeEntry = null
            activeTopic = null
        }
        screenId = id.orEmpty()
    }

    fun toggle(id: String) {
        if (enabled && screenId == id) {
            disable()
        } else {
            enabled = true
            screenId = id
            activeEntry = null
            activeTopic = null
        }
    }

    fun disable() {
        enabled = false
        activeEntry = null
        activeTopic = null
    }

    fun openEntry(entry: HelpEntry) {
        activeEntry = entry
    }

    fun openScreenGuide() {
        activeTopic = helpTopic(screenId)
    }

    fun dismissDialog() {
        activeEntry = null
        activeTopic = null
    }
}

val LocalHelpMode = androidx.compose.runtime.compositionLocalOf { HelpModeState() }
val LocalHelpScreenId = androidx.compose.runtime.compositionLocalOf { "" }

/**
 * Marks a control as a help target.
 *
 * The modifier only publishes bounds; the actual tap handling happens once
 * per screen in [Modifier.helpTapInterceptor], so adding an anchor never
 * changes the control's own click, gesture or scroll behavior.
 */
fun Modifier.helpAnchor(id: String, scopeOverride: String? = null): Modifier = composed {
    val state = LocalHelpMode.current
    val scope = scopeOverride ?: LocalHelpScreenId.current
    DisposableEffect(state, scope, id) {
        onDispose {
            if (scope.isNotBlank()) {
                state.unregister(scope, id)
            }
        }
    }
    this.onGloballyPositioned { coordinates ->
        if (scope.isNotBlank()) {
            state.register(scope, id, coordinates.boundsInRoot())
        }
    }
}

/**
 * Observes taps for the whole screen while help mode is on.
 *
 * It runs on the parent, does not consume the initial press and lets a drag
 * through untouched, so lists still scroll. A short tap consumes only the up
 * event, which prevents the control underneath from acting, and opens the
 * element description (or the screen guide when the tap landed on an
 * unannotated area).
 */
fun Modifier.helpTapInterceptor(state: HelpModeState): Modifier = composed {
    val enabled = state.enabled
    if (!enabled) return@composed this
    this
        .onGloballyPositioned { state.setLayerOrigin(it.boundsInRoot().topLeft) }
        .pointerInput(state.screenId) {
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial
                )
                var maxDistance = 0f
                var up: androidx.compose.ui.input.pointer.PointerInputChange? = null
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    maxDistance = maxOf(maxDistance, (change.position - down.position).getDistance())
                    if (!change.pressed) {
                        up = change
                        break
                    }
                }
                if (up != null && maxDistance <= viewConfiguration.touchSlop) {
                    up.consume()
                    state.tap(down.position)
                }
            }
        }
}

/** Wrapper for controls whose click handler is easier to keep untouched. */
@Composable
fun HelpAnchor(id: String, content: @Composable () -> Unit) {
    Box(modifier = Modifier.helpAnchor(id)) { content() }
}

@Composable
fun HelpModeOverlay(
    state: HelpModeState,
    onAllTopics: (String) -> Unit,
) {
    if (!state.enabled) return
    BackHandler { state.disable() }

    Box(modifier = Modifier.fillMaxSize().zIndex(3f)) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 12.dp, end = 12.dp, top = 72.dp, bottom = 8.dp)
                .widthIn(max = 560.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Help mode: tap a control",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { state.openScreenGuide() }) {
                    Text("Screen guide")
                }
                TextButton(onClick = { onAllTopics(state.screenId) }) {
                    Text("All topics")
                }
                TextButton(onClick = { state.disable() }) {
                    Text("Exit")
                }
            }
        }
    }

    state.activeEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { state.dismissDialog() },
            title = { Text(entry.title, fontWeight = FontWeight.SemiBold) },
            text = { Text(entry.body, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { state.dismissDialog() }) { Text("Got it") }
            },
        )
    }

    state.activeTopic?.let { topic ->
        HelpTopicDialog(topic = topic, onDismiss = { state.dismissDialog() })
    }
}

@Composable
private fun HelpTopicDialog(topic: HelpTopic, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(topic.title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(topic.intro, style = MaterialTheme.typography.bodyMedium)
                topic.sections.forEach { section ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = section.header,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(text = section.body, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
