package com.ai.assistance.operit.api.speech

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.preferences.SpeechServicesPreferences
import com.ai.assistance.operit.data.speech.CustomSttModels
import com.ai.assistance.operit.data.speech.GigaAMModelFiles
import com.ai.assistance.operit.data.speech.SttModelStorage
import com.ai.assistance.operit.util.AppLogger
import java.io.File
import kotlin.math.sqrt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Offline speech recognition: Silero VAD cuts the microphone stream into
 * phrases, GigaAM transcribes each finished phrase.
 *
 * GigaAM is a CTC model, not a streaming one, so there are no per-frame
 * partials. What the UI sees as it types is the committed text growing phrase
 * by phrase; a phrase appears when its trailing silence ends it. The audio of
 * an unfinished phrase is never dropped, only held: the VAD decides where an
 * utterance ends, and the buffer between commits is what gets transcribed.
 *
 * The model is fixed at 16 kHz. When the microphone refuses 16 kHz the capture
 * falls back to the device rate and [PcmResampler] converts on the fly, because
 * asking and hoping is what the sherpa path did and it fails on some hardware.
 *
 * Transcription runs on its own coroutine behind a channel so a slow decode
 * never stalls the capture loop and the microphone never overruns.
 */
@SuppressLint("MissingPermission")
class GigaAMSpeechProvider(
    private val context: Context,
    private val threads: Int = DEFAULT_THREADS,
) : SpeechService {

    companion object {
        private const val TAG = "GigaAMSpeechProvider"
        const val DEFAULT_THREADS = 2

        /** What the model eats: 16 kHz mono PCM16. */
        private const val TARGET_RATE = 16000

        /** Silero's native frame; VAD state only means anything at this size. */
        private const val VAD_FRAME = 512

        /** Audio kept before the detected onset so the first phoneme survives. */
        private const val PREROLL_SAMPLES = 8000 // 0.5 s

        /** Chunks shorter than this are VAD blips, not speech; skip the decode. */
        private const val MIN_UTTERANCE_SAMPLES = 3200 // 0.2 s

        /** Force-commit so one unpaused monologue cannot grow without bound. */
        private const val MAX_UTTERANCE_SAMPLES = 320_000 // 20 s

        /** Safety net for a VAD-off session, which is otherwise unbounded. */
        private const val MAX_UTTERANCE_OFF_SAMPLES = 960_000 // 60 s

        private const val READ_FRAMES = 2048
    }

    private val _recognitionState = MutableStateFlow(SpeechService.RecognitionState.UNINITIALIZED)
    override val currentState: SpeechService.RecognitionState
        get() = _recognitionState.value
    override val recognitionStateFlow: StateFlow<SpeechService.RecognitionState> =
        _recognitionState.asStateFlow()

    private val _recognitionResult = MutableStateFlow(SpeechService.RecognitionResult(""))
    override val recognitionResultFlow: StateFlow<SpeechService.RecognitionResult> =
        _recognitionResult.asStateFlow()

    private val _recognitionError = MutableStateFlow(SpeechService.RecognitionError(0, ""))
    override val recognitionErrorFlow: StateFlow<SpeechService.RecognitionError> =
        _recognitionError.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _volumeLevelFlow = MutableStateFlow(0f)
    override val volumeLevelFlow: StateFlow<Float> = _volumeLevelFlow.asStateFlow()

    override val isRecognizing: Boolean
        get() = currentState == SpeechService.RecognitionState.RECOGNIZING

    private var recognizer: GigaAMRecognizer? = null
    private var vad: OnnxSileroVad? = null
    private var audioRecord: AudioRecord? = null
    private var resampler: PcmResampler? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val initMutex = Mutex()
    private val sessionMutex = Mutex()
    private val committedMutex = Mutex()
    private val committed = StringBuilder()

    private var captureJob: Job? = null
    private var decodeJob: Job? = null
    private var chunkChannel: Channel<ShortArray>? = null
    /** Read fresh at every session start; the capture loop only reads it. */
    private var sessionVadEnabled = true
    /**
     * The live chunker. It belongs to the capture coroutine while it runs;
     * stop() joins the capture first, so flushing it in flushTail() after the
     * join is race-free and the half-spoken tail is not lost.
     */
    private var sessionChunker: VadUtteranceChunker? = null
    private var wantPartialResults = true
    private var smoothedVolume = 0f

    override suspend fun initialize(): Boolean {
        if (isInitialized.value) return true
        return initMutex.withLock {
            if (isInitialized.value) return@withLock true
            AppLogger.d(TAG, "Initializing GigaAM...")
            try {
                withContext(Dispatchers.IO) {
                    val prefs = SpeechServicesPreferences(context)
                    val modelId = prefs.localSttModelIdFlow.first()
                    val opened = openSelectedCheckpoint(modelId, threads)
                    if (opened == null) {
                        // Models download only with the user's explicit consent
                        // on their own screens, so reaching here without files
                        // is a normal state, not a crash.
                        val message = context.getString(R.string.stt_model_unavailable)
                        AppLogger.w(TAG, "Model files absent for '$modelId': $message")
                        _recognitionState.value = SpeechService.RecognitionState.ERROR
                        _recognitionError.value = SpeechService.RecognitionError(-1, message)
                        return@withContext false
                    }
                    // The VAD is built per session in startRecognition, not here:
                    // its mode and endpoint patience come from settings and must
                    // be fresh every time, and building it is cheap next to the
                    // 225 MB recognizer above.
                    recognizer = opened
                    AppLogger.d(TAG, "GigaAM initialized")
                    _isInitialized.value = true
                    _recognitionState.value = SpeechService.RecognitionState.IDLE
                    true
                }
            } catch (e: CancellationException) {
                // The owning screen left; a cancelled init is not a broken engine.
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to initialize GigaAM", e)
                _recognitionState.value = SpeechService.RecognitionState.ERROR
                _recognitionError.value =
                    SpeechService.RecognitionError(-1, e.message ?: "Unknown error")
                false
            }
        }
    }

    override suspend fun startRecognition(
        languageCode: String,
        continuousMode: Boolean,
        partialResults: Boolean,
        audioSource: Int,
    ): Boolean {
        return sessionMutex.withLock {
            if (!isInitialized.value) {
                if (!initialize()) return@withLock false
            }
            if (captureJob?.isActive == true ||
                currentState == SpeechService.RecognitionState.RECOGNIZING
            ) {
                return@withLock false
            }

            clearAndReleaseAudioRecord()
            _recognitionState.value = SpeechService.RecognitionState.PREPARING
            _recognitionResult.value =
                SpeechService.RecognitionResult(text = "", isFinal = false, confidence = 0f)
            committedMutex.withLock { committed.clear() }
            wantPartialResults = partialResults
            smoothedVolume = 0f

            // Tuning is read fresh every session. The microphone source for the
            // local engine comes from settings, not from the caller's
            // audioSource: callers leave the interface default, and two
            // authorities for one microphone would disagree silently.
            val tuning = withContext(Dispatchers.IO) {
                SpeechServicesPreferences(context).localSttTuningFlow.first()
            }
            sessionVadEnabled = tuning.vadEnabled
            try {
                vad?.close()
            } catch (_: Exception) {
            }
            val sessionVad = try {
                OnnxSileroVad(
                    context,
                    mode = if (tuning.vadAggressive) {
                        OnnxSileroVad.Mode.AGGRESSIVE
                    } else {
                        OnnxSileroVad.Mode.NORMAL
                    },
                    silenceDurationMs = tuning.endpointSilenceMs,
                ).also { it.reset() }
            } catch (e: Exception) {
                AppLogger.e(TAG, "VAD unavailable, session cannot chunk", e)
                _recognitionState.value = SpeechService.RecognitionState.ERROR
                _recognitionError.value =
                    SpeechService.RecognitionError(-1, e.message ?: "Unknown error")
                return@withLock false
            }
            vad = sessionVad

            val opened = openAudioRecord(tuning.micSource) ?: return@withLock false
            audioRecord = opened.record
            resampler = opened.resampler

            val channel = Channel<ShortArray>(Channel.UNLIMITED)
            chunkChannel = channel
            decodeJob = scope.launch { decodeLoop(channel) }
            captureJob = scope.launch(Dispatchers.IO) {
                captureLoop(continuousMode)
            }

            _recognitionState.value = SpeechService.RecognitionState.RECOGNIZING
            AppLogger.d(
                TAG,
                "Capture at ${opened.rate} Hz" +
                    if (opened.resampler != null) " (resampled to $TARGET_RATE Hz)" else "",
            )
            true
        }
    }

    override suspend fun stopRecognition(): Boolean {
        return sessionMutex.withLock {
            val capturing = captureJob?.isActive == true ||
                currentState == SpeechService.RecognitionState.RECOGNIZING
            if (!capturing) return@withLock false
            AppLogger.d(TAG, "Stopping recognition...")
            _recognitionState.value = SpeechService.RecognitionState.PROCESSING

            try {
                audioRecord?.stop()
            } catch (e: Exception) {
                AppLogger.w(TAG, "Error stopping AudioRecord", e)
            }
            captureJob?.cancelAndJoin()
            captureJob = null

            // The tail may hold speech the VAD never got to endpoint.
            flushTail()
            chunkChannel?.close()
            try {
                decodeJob?.join()
            } catch (_: Exception) {
            }
            decodeJob = null
            chunkChannel = null

            val text = committedMutex.withLock { committed.toString() }
            _recognitionResult.value =
                SpeechService.RecognitionResult(text = text, isFinal = true)
            clearAndReleaseAudioRecord()
            _recognitionState.value = SpeechService.RecognitionState.IDLE
            _volumeLevelFlow.value = 0f
            true
        }
    }

    override suspend fun cancelRecognition() {
        sessionMutex.withLock {
            captureJob?.cancelAndJoin()
            captureJob = null
            decodeJob?.cancelAndJoin()
            decodeJob = null
            chunkChannel?.cancel()
            chunkChannel = null
            sessionChunker = null
            clearAndReleaseAudioRecord()
            committedMutex.withLock { committed.clear() }
            _recognitionResult.value =
                SpeechService.RecognitionResult(text = "", isFinal = false, confidence = 0f)
            _recognitionState.value = SpeechService.RecognitionState.IDLE
            _volumeLevelFlow.value = 0f
            try {
                vad?.reset()
            } catch (_: Exception) {
            }
        }
    }

    override fun shutdown() {
        runBlocking {
            try {
                cancelRecognition()
            } catch (_: Exception) {
            }
            withContext(Dispatchers.IO) {
                try {
                    recognizer?.close()
                } catch (_: Exception) {
                }
                recognizer = null
                try {
                    vad?.close()
                } catch (_: Exception) {
                }
                vad = null
            }
            _isInitialized.value = false
            _recognitionState.value = SpeechService.RecognitionState.UNINITIALIZED
            _volumeLevelFlow.value = 0f
            _recognitionResult.value =
                SpeechService.RecognitionResult(text = "", isFinal = false, confidence = 0f)
        }
    }

    override suspend fun getSupportedLanguages(): List<String> =
        withContext(Dispatchers.IO) {
            // Only ru and en have been measured end to end; the graph is
            // multilingual but unmeasured languages stay out of the list.
            listOf("ru", "en")
        }

    override suspend fun recognize(audioData: FloatArray) {
        if (!isInitialized.value) {
            if (!initialize()) return
        }
        val instance = recognizer
        if (instance == null) {
            _recognitionState.value = SpeechService.RecognitionState.ERROR
            _recognitionError.value =
                SpeechService.RecognitionError(-1, "Recognizer not initialized")
            return
        }
        // Unlike the streaming providers, the offline model implements the batch
        // call directly instead of reporting it unsupported.
        val pcm = ShortArray(audioData.size) { i ->
            (audioData[i] * 32768.0f).toInt().coerceIn(-32768, 32767).toShort()
        }
        _recognitionState.value = SpeechService.RecognitionState.PROCESSING
        try {
            val text = withContext(Dispatchers.Default) { instance.transcribe(pcm) }
            _recognitionResult.value =
                SpeechService.RecognitionResult(text = text, isFinal = true)
            _recognitionState.value = SpeechService.RecognitionState.IDLE
        } catch (e: Exception) {
            AppLogger.e(TAG, "Batch recognition failed", e)
            _recognitionState.value = SpeechService.RecognitionState.ERROR
            _recognitionError.value =
                SpeechService.RecognitionError(-1, e.message ?: "Unknown error")
        }
    }

    /**
     * Opens the checkpoint the user picked in settings. An unknown id means
     * its entry is gone, so the built-in model takes over rather than
     * failing every session; reinstalling the entry restores it because the
     * stored id is left untouched.
     *
     * @return the recognizer, or null when its files are not downloaded
     */
    private fun openSelectedCheckpoint(modelId: String, threads: Int): GigaAMRecognizer? {
        if (modelId != SpeechServicesPreferences.BUILTIN_GIGAAM_ID) {
            val custom = CustomSttModels(context)
            val entry = custom.get(modelId)
            if (entry != null) {
                val dir = custom.downloadedDir(entry)
                if (dir != null) {
                    AppLogger.d(TAG, "Opening custom checkpoint '${entry.displayName}'")
                    return GigaAMRecognizer.openCustom(
                        modelFile = File(dir, CustomSttModels.MODEL_FILE_NAME),
                        tokensFile = File(dir, CustomSttModels.TOKENS_FILE_NAME),
                        config = entry.config,
                        threads = threads,
                    )
                }
                return null
            }
            AppLogger.w(TAG, "Unknown local checkpoint '$modelId', falling back to built-in")
        }
        val dir = SttModelStorage(context).currentDir()
        val modelFile = dir?.let { File(it, GigaAMModelFiles.MODEL_FILE_NAME) }
        val tokensFile = dir?.let { File(it, GigaAMModelFiles.TOKENS_FILE_NAME) }
        if (dir == null || modelFile == null || !modelFile.isFile ||
            tokensFile == null || !tokensFile.isFile
        ) {
            return null
        }
        return GigaAMRecognizer.open(dir, threads)
    }

    private data class OpenedCapture(
        val record: AudioRecord,
        val rate: Int,
        val resampler: PcmResampler?,
    )

    /**
     * 16 kHz first because that is what the model eats; the device rate with a
     * resampler when the hardware refuses it. Each rate is probed the same way
     * the old provider probed its only rate, so a dead microphone still fails
     * here instead of recording silence.
     */
    private fun openAudioRecord(audioSource: Int): OpenedCapture? {
        val channel = AudioFormat.CHANNEL_IN_MONO
        val format = AudioFormat.ENCODING_PCM_16BIT
        for (rate in intArrayOf(TARGET_RATE, 48000, 44100)) {
            val minBytes = try {
                AudioRecord.getMinBufferSize(rate, channel, format)
            } catch (_: Exception) {
                -1
            }
            if (minBytes <= 0) continue
            val record = try {
                AudioRecord(
                    audioSource,
                    rate,
                    channel,
                    format,
                    (minBytes * 4).coerceAtLeast(READ_FRAMES * 2 * 2),
                )
            } catch (_: Exception) {
                null
            }
            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                try {
                    record?.release()
                } catch (_: Exception) {
                }
                continue
            }
            try {
                record.startRecording()
            } catch (e: Exception) {
                AppLogger.w(TAG, "AudioRecord.startRecording failed at $rate Hz", e)
                try {
                    record.release()
                } catch (_: Exception) {
                }
                continue
            }
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                try {
                    record.release()
                } catch (_: Exception) {
                }
                continue
            }
            val converter = if (rate == TARGET_RATE) null else PcmResampler(rate, TARGET_RATE)
            return OpenedCapture(record, rate, converter)
        }
        AppLogger.e(TAG, "No usable AudioRecord rate")
        _recognitionState.value = SpeechService.RecognitionState.ERROR
        _recognitionError.value = SpeechService.RecognitionError(-3, "AudioRecord not initialized")
        return null
    }

    private fun clearAndReleaseAudioRecord() {
        val record = audioRecord
        audioRecord = null
        resampler = null
        if (record != null) {
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Error stopping AudioRecord", e)
            }
            try {
                record.release()
            } catch (e: Exception) {
                AppLogger.w(TAG, "Error releasing AudioRecord", e)
            }
        }
    }

    private suspend fun CoroutineScope.captureLoop(continuousMode: Boolean) {
        val record = audioRecord
        val vadInstance = vad
        if (record == null || vadInstance == null) {
            AppLogger.e(TAG, "Capture started without AudioRecord or VAD")
            return
        }
        val raw = ShortArray(READ_FRAMES)
        val chunker = VadUtteranceChunker(
            prerollSamples = PREROLL_SAMPLES,
            minUtteranceSamples = MIN_UTTERANCE_SAMPLES,
            // VAD off means one decode per session: the ceiling is a safety
            // net, not a phrase length, so it sits far out.
            maxUtteranceSamples = if (sessionVadEnabled) {
                MAX_UTTERANCE_SAMPLES
            } else {
                MAX_UTTERANCE_OFF_SAMPLES
            },
        )
        sessionChunker = chunker
        val carry = ShortArray(VAD_FRAME)
        var carrySize = 0
        var saidSomething = false

        try {
            while (isActive &&
                currentState == SpeechService.RecognitionState.RECOGNIZING
            ) {
                val read = try {
                    record.read(raw, 0, raw.size)
                } catch (e: Exception) {
                    AppLogger.w(TAG, "AudioRecord.read failed", e)
                    break
                }
                if (read <= 0) break

                _volumeLevelFlow.value = updateVolume(raw, read)

                val at16k: ShortArray
                val at16kSize: Int
                val converter = resampler
                if (converter == null) {
                    at16k = raw
                    at16kSize = read
                } else {
                    val converted = converter.processInt16(raw.copyOf(read))
                    at16k = converted
                    at16kSize = converted.size
                }

                var offset = 0
                // Top up the carried-over partial frame first.
                if (carrySize > 0) {
                    val need = VAD_FRAME - carrySize
                    val take = minOf(need, at16kSize)
                    System.arraycopy(at16k, 0, carry, carrySize, take)
                    carrySize += take
                    offset += take
                    if (carrySize == VAD_FRAME) {
                        if (stepFrame(vadInstance, carry, chunker)) saidSomething = true
                        carrySize = 0
                        if (!chunker.inSpeech && !continuousMode && saidSomething) return
                    }
                }
                while (offset + VAD_FRAME <= at16kSize) {
                    val frame = at16k.copyOfRange(offset, offset + VAD_FRAME)
                    if (stepFrame(vadInstance, frame, chunker)) saidSomething = true
                    offset += VAD_FRAME
                    if (!chunker.inSpeech && !continuousMode && saidSomething) return
                }
                if (offset < at16kSize) {
                    val rest = at16kSize - offset
                    System.arraycopy(at16k, offset, carry, 0, rest)
                    carrySize = rest
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Capture loop failed", e)
        } finally {
            _volumeLevelFlow.value = 0f
        }
    }

    /**
     * One frame through the VAD and the chunker.
     *
     * @return true when this frame finished an utterance worth decoding
     */
    private fun stepFrame(
        vadInstance: OnnxSileroVad,
        frame: ShortArray,
        chunker: VadUtteranceChunker,
    ): Boolean {
        // VAD off is not "no boundaries": without verdicts an offline model
        // cannot chunk at all, so the session becomes one utterance decoded on
        // stop (or at the far ceiling). Best accuracy per decode, no partials.
        val speech = if (!sessionVadEnabled) {
            true
        } else try {
            vadInstance.isSpeech(frame)
        } catch (e: Exception) {
            AppLogger.w(TAG, "VAD failed on a frame", e)
            chunker.inSpeech
        }
        val done = chunker.feed(frame, speech) ?: return false
        // Sealed on the capture side after the loop dies in stop(), so trySend on
        // an unlimited channel cannot fail here; a dropped send means the session
        // is already gone and the audio is no one's.
        chunkChannel?.trySend(done)
        return true
    }

    /**
     * Called after the capture loop is joined in stop(): whatever speech never
     * reached an endpoint becomes the last chunk of the session. Silence held
     * as preroll is dropped, not decoded.
     */
    private fun flushTail() {
        val chunker = sessionChunker
        sessionChunker = null
        if (chunker == null) return
        val tail = try {
            resampler?.flush()?.toShortArray()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Resampler flush failed", e)
            null
        }
        // Chronological order: the buffered speech first, the filter ring-down
        // after it.
        if (tail != null && tail.isNotEmpty()) {
            chunker.appendRaw(tail)
        }
        chunker.flush()?.let { chunkChannel?.trySend(it) }
    }

    private suspend fun decodeLoop(channel: Channel<ShortArray>) {
        val instance = recognizer ?: return
        try {
            for (chunk in channel) {
                val audioSeconds = chunk.size / 16000.0
                val startedAt = SystemClock.elapsedRealtime()
                val text = try {
                    withContext(Dispatchers.Default) { instance.transcribe(chunk) }
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Utterance decode failed", e)
                    continue
                }
                // Wall time per phrase, measured on the phone: the only speed
                // number that matters for this engine.
                val decodeMs = SystemClock.elapsedRealtime() - startedAt
                if (text.isBlank()) {
                    AppLogger.d(
                        TAG,
                        "Blank phrase: ${"%.1f".format(audioSeconds)} s audio in $decodeMs ms",
                    )
                    continue
                }
                val full = committedMutex.withLock {
                    if (committed.isNotEmpty()) committed.append(' ')
                    committed.append(text)
                    committed.toString()
                }
                _recognitionResult.value =
                    SpeechService.RecognitionResult(text = full, isFinal = false)
                AppLogger.d(
                    TAG,
                    "Phrase (${"%.1f".format(audioSeconds)} s audio in $decodeMs ms): $text",
                )
            }
        } catch (e: CancellationException) {
            // Channel cancelled under our feet by cancelRecognition; the session
            // is gone and there is nothing to report.
            throw e
        } catch (_: Exception) {
            // A single bad chunk must not kill the session.
        }
    }

    private fun DoubleArray.toShortArray(): ShortArray =
        ShortArray(size) { i ->
            (this[i] * 32768.0).toInt().coerceIn(-32768, 32767).toShort()
        }

    private fun updateVolume(buffer: ShortArray, length: Int): Float {
        var sum = 0.0
        for (i in 0 until length) {
            val s = buffer[i] / 32768.0
            sum += s * s
        }
        val rms = sqrt(sum / length.coerceAtLeast(1)).toFloat()
        // Speech at conversational level sits well below full scale; scale so the
        // indicator visibly moves without pinning on loud input.
        smoothedVolume += (rms * 4f - smoothedVolume) * 0.15f
        return smoothedVolume.coerceIn(0f, 1f)
    }
}
