package com.ai.assistance.operit.ui.features.llmio

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ai.assistance.operit.data.model.LlmIoLogEntity
import com.ai.assistance.operit.data.stats.LlmIoLogRepository
import com.ai.assistance.operit.util.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LlmIoLogUiState(
    val loading: Boolean = true,
    val entries: List<LlmIoLogEntity> = emptyList(),
    val models: List<String> = emptyList(),
    val selectedModel: String? = null,
    val enabled: Boolean = true,
    val totalCount: Long = 0L,
    val hasMore: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Backs the LLM I/O log screen: paged rows, model filter, logging toggle,
 * per-row delete and full clear. All Room access stays on Dispatchers.IO.
 */
class LlmIoLogViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val repository = LlmIoLogRepository.getInstance(appContext)

    private val _state = MutableStateFlow(LlmIoLogUiState())
    val state: StateFlow<LlmIoLogUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(loading = true, errorMessage = null) }
            try {
                val enabled = repository.isEnabled()
                val models = repository.models()
                val selected = _state.value.selectedModel?.takeIf { it in models }
                val entries = repository.recent(selected, PAGE_SIZE, 0)
                val total = repository.count()
                _state.update {
                    it.copy(
                        loading = false,
                        entries = entries,
                        models = models,
                        selectedModel = selected,
                        enabled = enabled,
                        totalCount = total,
                        hasMore = entries.size >= PAGE_SIZE,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "llm io log load failed", e)
                _state.update {
                    it.copy(loading = false, errorMessage = e.message ?: e.javaClass.simpleName)
                }
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.loading || !current.hasMore) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val next =
                    repository.recent(current.selectedModel, PAGE_SIZE, current.entries.size)
                _state.update {
                    it.copy(
                        entries = it.entries + next,
                        hasMore = next.size >= PAGE_SIZE,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "llm io log page load failed", e)
            }
        }
    }

    fun selectModel(model: String?) {
        if (_state.value.selectedModel == model) return
        _state.update { it.copy(selectedModel = model, entries = emptyList()) }
        refresh()
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.setEnabled(enabled)
                _state.update { it.copy(enabled = enabled) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "llm io log toggle failed", e)
            }
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.delete(id)
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "llm io log delete failed", e)
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clear()
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "llm io log clear failed", e)
            }
        }
    }

    /** Plain-text dump of one row for the share sheet. */
    fun buildExportText(entry: LlmIoLogEntity): String =
        buildString {
            appendLine("model: ${entry.modelId}")
            appendLine("function: ${entry.function}")
            appendLine("time: ${java.time.Instant.ofEpochMilli(entry.timestampMs)}")
            appendLine(
                "tokens: prompt=${entry.promptTokens ?: "?"} " +
                    "completion=${entry.completionTokens ?: "?"}"
            )
            appendLine("latency: ${entry.latencyMs?.let { "${it}ms" } ?: "?"}")
            entry.error?.let { appendLine("error: $it") }
            if (entry.function == LlmIoLogRepository.FUNCTION_SUPERVISION) {
                appendLine("verdict: ${entry.supervisionVerdict ?: "?"}")
                entry.supervisionComment?.let { appendLine("comment: $it") }
                entry.supervisionCorrectedCall?.let { appendLine("corrected_call: $it") }
            }
            appendLine()
            appendLine("--- request ---")
            appendLine(entry.requestJson)
            appendLine()
            appendLine("--- response ---")
            append(entry.responseText)
        }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LlmIoLogViewModel(appContext) as T
    }

    companion object {
        private const val TAG = "LlmIoLogViewModel"
        const val PAGE_SIZE = 50
    }
}
