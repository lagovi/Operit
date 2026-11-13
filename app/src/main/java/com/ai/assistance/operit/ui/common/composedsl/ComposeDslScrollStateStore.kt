package com.ai.assistance.operit.ui.common.composedsl

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * The Compose DSL UI tree is fully rebuilt after every JS state update.
 *
 * A LazyColumn/LazyRow LazyListState is only remembered at its current composition
 * position, so if the list node leaves composition (an outer conditional branch, a page
 * switch or a dialog rebuild) the state is discarded with the composition and the scroll
 * position jumps back to the top.
 *
 * This store is owned by the DSL host (page and dialog); its lifetime is longer than the
 * UI tree itself. The list renderers fetch the same LazyListState back from the store by a
 * stable node identity, so ordinary state updates, popup rebuilds and page switches no
 * longer lose the scroll position.
 *
 * The node identity prefers an explicit key passed by the DSL; when there is no key it uses
 * the node path. The explicit key distinguishes multiple views in the same page that may
 * share a path (for example the worldbook list page and editor page).
 */
internal class ComposeDslScrollStateStore(
    restoredStates: Map<String, Pair<Int, Int>> = emptyMap()
) {
    private val lazyListStates =
        restoredStates.mapValuesTo(mutableMapOf()) { (_, position) ->
            LazyListState(position.first, position.second)
        }

    fun lazyListState(key: String): LazyListState = lazyListStates.getOrPut(key) { LazyListState() }

    fun saveState(): Map<String, List<Int>> =
        lazyListStates.mapValues { (_, state) ->
            listOf(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
        }

    companion object {
        val Saver: Saver<ComposeDslScrollStateStore, Map<String, List<Int>>> =
            Saver(
                save = { store -> store.saveState() },
                restore = { saved ->
                    ComposeDslScrollStateStore(
                        restoredStates =
                            saved.mapValues { (_, position) ->
                                position[0] to position[1]
                            }
                    )
                }
            )
    }
}

internal val LocalComposeDslScrollStateStore =
    compositionLocalOf<ComposeDslScrollStateStore?> { null }

@Composable
internal fun rememberComposeDslScrollStateStore(routeInstanceId: String): ComposeDslScrollStateStore =
    rememberSaveable(routeInstanceId, saver = ComposeDslScrollStateStore.Saver) {
        ComposeDslScrollStateStore()
    }

/**
 * Computes the stable identity of a list scroll state.
 *
 * The key contains the node path and the node type so that lists in different positions do
 * not share the same state; the explicit key participates in the identity so the DSL can
 * declare independent scroll positions for different views under the same path.
 */
internal fun composeDslLazyListStateKey(
    parentNodePath: String,
    nodeType: String,
    nodeKey: Any?
): String {
    val normalizedType = normalizeToken(nodeType)
    val explicitKey = nodeKey?.toString()?.trim()?.ifBlank { null }
    return if (explicitKey != null) {
        "$parentNodePath:$normalizedType:key:$explicitKey"
    } else {
        "$parentNodePath:$normalizedType"
    }
}

@Composable
internal fun rememberComposeDslLazyListState(
    nodePath: String,
    nodeType: String,
    nodeKey: Any?
): LazyListState {
    val stateKey = composeDslLazyListStateKey(nodePath, nodeType, nodeKey)
    val store = LocalComposeDslScrollStateStore.current
    return if (store != null) {
        store.lazyListState(stateKey)
    } else {
        rememberSaveable(stateKey, saver = LazyListState.Saver) { LazyListState() }
    }
}
