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
 * The one-time GigaAM download, shared by every screen that touches it.
 *
 * The model downloads only after the user explicitly agrees, in the dictation
 * screen or in settings, and declining agrees to nothing: no flag is stored,
 * the files simply stay absent, and settings keeps offering the download until
 * they exist. That is the whole "change your mind later" mechanism — there is
 * no declined state to undo.
 *
 * A cancelled or interrupted transfer resumes via HTTP Range on the next run,
 * so cancelling is free and retrying never redownloads verified bytes.
 */
class GigaAMModelDownload private constructor(context: Context) {

    sealed interface State {
        /** Nothing known yet; call [refresh]. */
        data object Unknown : State
        /** Files are being checked on disk. */
        data object Checking : State
        /** Model absent, nothing running. */
        data object Absent : State
        /** Transfer running. Bytes are cumulative across both files. */
        data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : State
        /** Both files present and verified. */
        data class Ready(val dir: File) : State
        /** The last run failed; the message is user-facing. */
        data class Failed(val message: String) : State
    }

    private val appContext = context.applicationContext
    private val storage = SttModelStorage(appContext)
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
            _state.value = State.Downloading(0L, GigaAMModelFiles.TOTAL_BYTES)
            val result = fetcher.ensureAssets(
                dir,
                GigaAMModelFiles.ASSETS,
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
        private var instance: GigaAMModelDownload? = null

        fun getInstance(context: Context): GigaAMModelDownload =
            instance ?: synchronized(this) {
                instance ?: GigaAMModelDownload(context).also { instance = it }
            }
    }
}
