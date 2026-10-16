package com.ai.assistance.operit.data.speech

import android.content.Context
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.util.RemoteAssetFetcher
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The one-time Parakeet download, shared by every screen that touches it.
 *
 * Same contract as [GigaAMModelDownload]: explicit user consent first, no
 * declined state, resume via HTTP Range. Separate instance because the
 * files, sizes and storage leaf differ.
 */
class ParakeetModelDownload private constructor(context: Context) {

    sealed interface State {
        /** Nothing known yet; call [refresh]. */
        data object Unknown : State
        /** Files are being checked on disk. */
        data object Checking : State
        /** Model absent, nothing running. */
        data object Absent : State
        /** Transfer running. Bytes are cumulative across all files. */
        data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : State
        /** All files present and verified. */
        data class Ready(val dir: File) : State
        /** The last run failed; the message is user-facing. */
        data class Failed(val message: String) : State
    }

    private val appContext = context.applicationContext
    private val storage = ParakeetModelStorage(appContext)
    private val fetcher = RemoteAssetFetcher(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<State>(State.Unknown)
    val state: StateFlow<State> = _state.asStateFlow()

    private var job: Job? = null

    /** Re-read the disk; cheap, checks sizes only, never hashes. */
    fun refresh() {
        if (job?.isActive == true) return
        scope.launch(Dispatchers.IO) {
            _state.value = State.Checking
            _state.value = storage.isDownloaded()?.let { State.Ready(it) } ?: State.Absent
        }
    }

    /**
     * Fetch and verify the model into the current storage location.
     * No-op when already running. Cancellation resumes later, it does not
     * discard: verified files stay, the partial stays, nothing is deleted.
     */
    fun download() {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            val dir = storage.currentDir()
            if (dir == null) {
                _state.value = State.Failed("Storage unavailable")
                return@launch
            }
            _state.value = State.Downloading(0L, ParakeetModelFiles.TOTAL_BYTES)
            val result = fetcher.ensureAssets(
                dir,
                ParakeetModelFiles.ASSETS,
            ) { downloaded, total, _ ->
                _state.value = State.Downloading(downloaded, total)
            }
            _state.value = result.fold(
                onSuccess = { State.Ready(it) },
                onFailure = { State.Failed(it.message ?: "Download failed") },
            )
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        scope.launch(Dispatchers.IO) {
            // The transfer stopped mid-file; re-read so the UI shows Absent
            // rather than a frozen progress bar. Partial bytes stay on disk
            // for the next run to resume.
            if (_state.value is State.Downloading) {
                _state.value = State.Absent
            }
        }
    }

    companion object {
        @Volatile
        private var instance: ParakeetModelDownload? = null

        fun getInstance(context: Context): ParakeetModelDownload =
            instance ?: synchronized(this) {
                instance ?: ParakeetModelDownload(context).also { instance = it }
            }
    }
}
