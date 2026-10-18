package com.ai.assistance.operit.util

import android.content.Context
import com.ai.assistance.operit.terminal.RootfsArchiveProvider
import com.ai.assistance.operit.terminal.TerminalRootfs
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The one-time terminal rootfs transfer, shared by the terminal itself (through
 * [RootfsArchiveProvider], installed into [TerminalRootfs.provider] in
 * `OperitApplication.onCreate`) and by the UI gate that asks the user first.
 *
 * The archive is downloaded only after the user agrees, and only when Ubuntu is
 * not yet installed. A cancelled or interrupted transfer resumes via HTTP Range
 * on the next run, so cancelling costs nothing and never redownloads verified
 * bytes. After the install script unpacks Ubuntu it deletes the archive, and
 * [isInstalled] becomes the reason no further transfer happens.
 */
class TerminalRootfsDownload private constructor(context: Context) : RootfsArchiveProvider {

    sealed interface State {
        /** Nothing known yet; call [refresh]. */
        data object Unknown : State
        /** State is being read from disk. */
        data object Checking : State
        /** The terminal can start: Ubuntu installed, or the archive is present. */
        data object Ready : State
        /** Absent, nothing running; the user can start the download. */
        data object Absent : State
        /** Transfer running. */
        data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : State
        /** The last run failed; the message is user-facing. */
        data class Failed(val message: String) : State
    }

    private val appContext = context.applicationContext
    private val fetcher = RemoteAssetFetcher(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val transfer = Mutex()

    private val _state = MutableStateFlow<State>(State.Unknown)
    val state: StateFlow<State> = _state.asStateFlow()

    private var job: Job? = null

    private val archive: File
        get() = File(appContext.filesDir, TerminalRootfs.ARCHIVE_NAME)

    /** Re-read the disk; cheap, checks for the installed tree and the archive. */
    fun refresh() {
        if (job?.isActive == true) return
        scope.launch(Dispatchers.IO) {
            _state.value = State.Checking
            _state.value = if (isReady()) State.Ready else State.Absent
        }
    }

    /** Start the transfer; no-op when already running. */
    fun download() {
        if (job?.isActive == true) return
        job =
            scope.launch(Dispatchers.IO) {
                ensureArchive(archive)
            }
    }

    fun cancel() {
        job?.cancel()
        job = null
        scope.launch(Dispatchers.IO) {
            if (_state.value is State.Downloading) {
                _state.value = State.Absent
            }
        }
    }

    override suspend fun ensureArchive(destination: File): Result<File> =
        transfer.withLock {
            if (isReady()) {
                _state.value = State.Ready
                return@withLock Result.success(destination)
            }
            _state.value = State.Downloading(0L, OfflinePayload.TERMINAL_ROOTFS.downloadBytes)
            val result =
                fetcher.ensureAssets(
                    destination.parentFile ?: appContext.filesDir,
                    OfflinePayload.TERMINAL_ROOTFS.assets,
                ) { downloaded, total, _ ->
                    _state.value = State.Downloading(downloaded, total)
                }
            result
                .onSuccess { _state.value = State.Ready }
                .onFailure { _state.value = State.Failed(it.message ?: "Download failed") }
            result
        }

    private fun isReady(): Boolean =
        TerminalRootfs.isInstalled(appContext.filesDir) || archive.isFile

    companion object {
        @Volatile
        private var instance: TerminalRootfsDownload? = null

        fun getInstance(context: Context): TerminalRootfsDownload =
            instance ?: synchronized(this) {
                instance ?: TerminalRootfsDownload(context).also { instance = it }
            }
    }
}
