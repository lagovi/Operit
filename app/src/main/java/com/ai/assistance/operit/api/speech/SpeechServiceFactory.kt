package com.ai.assistance.operit.api.speech

import android.content.Context
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.data.preferences.SpeechServiceProfilesPreferences
import com.ai.assistance.operit.data.preferences.SpeechServicesPreferences.SttHttpConfig
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean

/** 语音识别服务工厂类 用于创建和管理不同类型的语音识别服务 */
object SpeechServiceFactory {
    private const val TAG = "SpeechServiceFactory"

    /** 语音识别服务类型 */
    enum class SpeechServiceType {
        /** GigaAM offline plus Silero VAD. */
        LOCAL_GIGAAM,
        /** Parakeet TDT offline plus Silero VAD. */
        LOCAL_PARAKEET,
        /** Nemotron 3.5 streaming transducer (sherpa-onnx) plus Silero VAD. */
        LOCAL_NEMOTRON,
        OPENAI_STT,
        DEEPGRAM_STT,
    }

    /**
     * 创建语音识别服务实例
     *
     * @param context 应用上下文
     * @return 对应类型的语音识别服务实例
     */
    fun createSpeechService(
        context: Context
    ): SpeechService {
        val profiles = SpeechServiceProfilesPreferences(context)
        val profile = runBlocking { profiles.getCurrentSttProfile() }

        return createSpeechService(context, profile.serviceType, profile.httpConfig)
    }

    fun createWakeSpeechService(
        context: Context,
    ): SpeechService {
        val profiles = SpeechServiceProfilesPreferences(context)
        val profile = runBlocking { profiles.getCurrentSttProfile() }
        val selectedType = profile.serviceType
        val effectiveType = when (selectedType) {
            SpeechServiceType.OPENAI_STT,
            SpeechServiceType.DEEPGRAM_STT,
            -> SpeechServiceType.LOCAL_GIGAAM
            else -> selectedType
        }
        return createSpeechService(context, effectiveType, profile.httpConfig)
    }

    fun createSpeechService(
        context: Context,
        type: SpeechServiceType,
    ): SpeechService {
        val profiles = SpeechServiceProfilesPreferences(context)
        val config = runBlocking { profiles.getCurrentSttProfile().httpConfig }
        return createSpeechService(context, type, config)
    }

    private fun createSpeechService(
        context: Context,
        type: SpeechServiceType,
        httpConfig: SttHttpConfig,
    ): SpeechService {
        return when (type) {
            SpeechServiceType.LOCAL_GIGAAM -> acquireLocalSpeechService(context, type)
            SpeechServiceType.LOCAL_PARAKEET -> acquireLocalSpeechService(context, type)
            SpeechServiceType.LOCAL_NEMOTRON -> acquireLocalSpeechService(context, type)
            SpeechServiceType.OPENAI_STT -> {
                runBlocking {
                    OpenAISttProvider(
                        context = context,
                        endpointUrl = httpConfig.endpointUrl,
                        apiKey = httpConfig.apiKey,
                        model = httpConfig.modelName,
                    )
                }
            }
            SpeechServiceType.DEEPGRAM_STT -> {
                runBlocking {
                    DeepgramSttProvider(
                        context = context,
                        endpointUrl = httpConfig.endpointUrl,
                        apiKey = httpConfig.apiKey,
                        model = httpConfig.modelName,
                    )
                }
            }
        }
    }

    private class SpeechServiceLease(
        private val delegate: SpeechService,
        private val onRelease: () -> Unit,
    ) : SpeechService by delegate {
        private val released = AtomicBoolean(false)

        override fun shutdown() {
            if (released.compareAndSet(false, true)) {
                onRelease()
            }
        }
    }

    private data class LocalEntry(
        val type: SpeechServiceType,
        val service: SpeechService,
        var refCount: Int,
    )

    private val localLock = Any()
    private var localEntry: LocalEntry? = null

    private fun acquireLocalSpeechService(
        context: Context,
        type: SpeechServiceType,
    ): SpeechService {
        val appContext = context.applicationContext
        var stale: SpeechService? = null
        val entry =
            synchronized(localLock) {
                val existing = localEntry
                if (existing != null && existing.type != type) {
                    // On-device checkpoints are big enough that only one may be
                    // resident. A live lease means someone is still using it;
                    // a released (keep-alive) one is dropped below the lock.
                    if (existing.refCount > 0) {
                        throw IllegalStateException(
                            "Local SpeechService already active: ${existing.type}. " +
                                "Cannot create another local SpeechService of type $type before releasing the previous one."
                        )
                    }
                    localEntry = null
                    stale = existing.service
                }

                val current = localEntry
                if (current != null) {
                    current.refCount += 1
                    current
                } else {
                    val service =
                        when (type) {
                            SpeechServiceType.LOCAL_GIGAAM -> GigaAMSpeechProvider(appContext)
                            SpeechServiceType.LOCAL_PARAKEET -> ParakeetSpeechProvider(appContext)
                            SpeechServiceType.LOCAL_NEMOTRON -> NemotronSpeechProvider(appContext)
                            else -> throw IllegalArgumentException("Not a local SpeechService type: $type")
                        }
                    LocalEntry(type = type, service = service, refCount = 1).also { localEntry = it }
                }
            }

        try {
            stale?.shutdown()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to drop the previous local SpeechService", e)
        }

        return SpeechServiceLease(
            delegate = entry.service,
            onRelease = { releaseLocalSpeechService(entry.type) },
        )
    }

    /**
     * Switches the resident local engine without leaving a stale lease behind.
     *
     * The demo screen, the cached singleton and the floating voice helper all
     * hold leases over the same provider. A plain [createSpeechService] refuses
     * to start a second checkpoint while any lease is alive, which used to
     * crash the UI when the user changed the engine. Switching always stops
     * the cached lease first; a local checkpoint that is not recognizing is
     * then closed so the new engine can take its place. A session that is
     * genuinely running is left alone and reported to the caller.
     */
    fun switchLocalSpeechService(
        context: Context,
        type: SpeechServiceType,
    ): SpeechService {
        if (type != SpeechServiceType.LOCAL_GIGAAM &&
            type != SpeechServiceType.LOCAL_PARAKEET &&
            type != SpeechServiceType.LOCAL_NEMOTRON
        ) {
            return createSpeechService(context, type)
        }

        // Drop the factory-owned lease first. It is the one that survived the
        // old screen's shutdown and made refCount > 0.
        resetInstance()

        val stale =
            synchronized(localLock) {
                val current = localEntry
                if (current != null && current.type != type) {
                    if (current.service.isRecognizing) {
                        throw IllegalStateException(
                            "Stop the current recognition before switching the on-device engine."
                        )
                    }
                    localEntry = null
                    current.service
                } else {
                    null
                }
            }

        try {
            stale?.shutdown()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to close the previous local SpeechService", e)
        }

        return createSpeechService(context, type)
    }

    private fun releaseLocalSpeechService(type: SpeechServiceType) {
        synchronized(localLock) {
            val entry = localEntry
            if (entry == null || entry.type != type) {
                return
            }
            if (entry.refCount > 0) {
                entry.refCount -= 1
            }
            // Keep-alive: at zero the loaded checkpoint stays resident (the
            // entry and the recognizer are kept) so the next session starts
            // without the multi-second load. It is dropped only by
            // unloadLocalService() or by switching to another local engine.
        }
    }

    // 单例实例缓存
    private var instance: SpeechService? = null
    private var currentProfileId: String? = null

    /**
     * 获取语音识别服务单例实例
     *
     * @param context 应用上下文
     * @return 语音识别服务实例
     */
    fun getInstance(
        context: Context,
    ): SpeechService {
        val profiles = SpeechServiceProfilesPreferences(context)
        val profile = runBlocking { profiles.getCurrentSttProfile() }
        val selectedProfileId = profile.id

        val needNewInstance = instance == null || selectedProfileId != currentProfileId
        
        if (needNewInstance) {
            try {
                instance?.shutdown()
            } catch (_: Exception) {
            }

            val created =
                try {
                    createSpeechService(context)
                } catch (e: IllegalStateException) {
                    AppLogger.w(TAG, "Failed to create SpeechService for profile=$selectedProfileId, keeping previous instance", e)
                    null
                }

            if (created != null) {
                instance = created
                currentProfileId = selectedProfileId
            }
        }

        val value = instance
        if (value != null) return value

        val fallbackEntry = synchronized(localLock) { localEntry }
        if (fallbackEntry != null) {
            return acquireLocalSpeechService(context, fallbackEntry.type)
        }

        return createSpeechService(context, SpeechServiceType.LOCAL_GIGAAM)
    }

    /** 重置单例实例 在需要更改语音识别服务类型或释放资源时调用 */
    fun resetInstance() {
        try {
            instance?.shutdown()
        } catch (_: Exception) {
        }
        instance = null
        currentProfileId = null
    }

    /** Outcome of [unloadLocalService], for the settings screen's status line. */
    enum class LocalUnloadResult {
        /** No local checkpoint was held in memory. */
        NOT_LOADED,
        /** The checkpoint was released; its RAM is back. */
        UNLOADED,
        /** A session is running or another consumer still holds the engine. */
        IN_USE,
        /** Releasing threw; the process memory state is unknown. */
        FAILED,
    }

    /**
     * Whether a local engine currently keeps its checkpoint in memory. This is
     * the 214–670 MB the settings screen lets the user reclaim: a provider
     * object exists as soon as a screen asks for it, but only `initialize()`
     * loads the weights.
     */
    fun isLocalServiceLoaded(): Boolean =
        synchronized(localLock) { localEntry?.service?.isInitialized?.value == true }

    /**
     * Drops the in-memory local checkpoint so its weights stop costing RAM.
     *
     * Refuses while a recognition session is live or when more than one
     * consumer holds a lease: pulling the model out from under either would
     * break it. The downloaded files are untouched, so the next session
     * reloads them (the few-second `PREPARING` delay comes back with it).
     *
     * Blocking (`shutdown()` joins the recognition coroutines); call off the
     * main thread.
     */
    fun unloadLocalService(): LocalUnloadResult {
        val entry =
            synchronized(localLock) {
                val current = localEntry ?: return LocalUnloadResult.NOT_LOADED
                if (current.refCount > 1 || current.service.isRecognizing) {
                    return LocalUnloadResult.IN_USE
                }
                if (!current.service.isInitialized.value) {
                    return LocalUnloadResult.NOT_LOADED
                }
                localEntry = null
                current
            }
        // The cached singleton is a lease over the same provider; drop it too,
        // or the next getInstance() would hand out a released recognizer.
        instance = null
        currentProfileId = null
        return try {
            entry.service.shutdown()
            LocalUnloadResult.UNLOADED
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to unload local SpeechService", e)
            LocalUnloadResult.FAILED
        }
    }
}
