package com.ai.assistance.operit.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ai.assistance.operit.api.speech.SpeechServiceFactory
import com.ai.assistance.operit.api.voice.HttpTtsResponsePipelineStep
import com.ai.assistance.operit.api.voice.VoiceServiceFactory

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.speechServicesDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "speech_services_preferences")

/**
 * Legacy single-config store for speech services.
 *
 * New code manages profiles through [SpeechServiceProfilesPreferences]. This store remains the
 * active-profile projection consumed by existing providers and published-version integrations.
 */
class SpeechServicesPreferences(private val context: Context) {

    private val dataStore = context.speechServicesDataStore
    private val serializerJson = Json { ignoreUnknownKeys = true }

    @Serializable
    data class TtsHttpConfig(
        val urlTemplate: String,
        val apiKey: String, // Keep apiKey for header-based auth
        val headers: Map<String, String>,
        val httpMethod: String = "GET", // HTTP方法：GET 或 POST
        val requestBody: String = "", // POST请求的body模板，支持占位符如{text}
        val contentType: String = "application/json", // POST请求的Content-Type
        val localeTag: String = "", // 通用 TTS 语言标签，如 zh-CN、en-US
        val voiceId: String = "", // 特定于TTS提供商的音色ID
        val modelName: String = "", // TTS模型名称（用于SiliconFlow等）
        val responsePipeline: List<HttpTtsResponsePipelineStep> = emptyList()
    )

    @Serializable
    data class VitsTtsPackageConfig(
        val packagePath: String = "",
        val speakerId: String = "",
        val options: Map<String, String> = emptyMap()
    )

    @Serializable
    data class SttHttpConfig(
        val endpointUrl: String,
        val apiKey: String,
        val modelName: String,
    )

    /**
     * Tuning for the on-device engine, global rather than per-profile: the
     * microphone and the chunker do not belong to any single profile.
     * Defaults reproduce the behavior the engine shipped with, so upgrading
     * changes nothing until the user touches these.
     */
    data class LocalSttTuning(
        /** Split speech into phrases; off means one decode per session. */
        val vadEnabled: Boolean = true,
        /** Aggressive VAD rejects more non-speech at the cost of harder cuts. */
        val vadAggressive: Boolean = false,
        /** Trailing silence that ends a phrase, in milliseconds. */
        val endpointSilenceMs: Int = 300,
        /** MediaRecorder AudioSource value for the microphone. */
        val micSource: Int = android.media.MediaRecorder.AudioSource.VOICE_COMMUNICATION,
    ) {
        companion object {
            val ENDPOINT_OPTIONS_MS = listOf(300, 500, 800)
        }
    }

    companion object {
        // TTS Preference Keys
        val TTS_SERVICE_TYPE = stringPreferencesKey("tts_service_type")
        val TTS_HTTP_CONFIG = stringPreferencesKey("tts_http_config")
        val TTS_VITS_PACKAGE_CONFIG = stringPreferencesKey("tts_vits_package_config")
        val TTS_CLEANER_REGEXS = stringSetPreferencesKey("tts_cleaner_regexs")
        val TTS_SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")

        // STT Preference Keys
        val STT_SERVICE_TYPE = stringPreferencesKey("stt_service_type")
        val STT_HTTP_CONFIG = stringPreferencesKey("stt_http_config")

        // Default Values
        val DEFAULT_TTS_SERVICE_TYPE = VoiceServiceFactory.VoiceServiceType.SIMPLE_TTS
        val DEFAULT_STT_SERVICE_TYPE = SpeechServiceFactory.SpeechServiceType.LOCAL_GIGAAM

        val DEFAULT_LOCAL_STT_TUNING = LocalSttTuning()

        private val STT_VAD_ENABLED = booleanPreferencesKey("stt_vad_enabled")
        private val STT_VAD_AGGRESSIVE = booleanPreferencesKey("stt_vad_aggressive")
        private val STT_ENDPOINT_SILENCE_MS = intPreferencesKey("stt_endpoint_silence_ms")
        private val STT_MIC_SOURCE = intPreferencesKey("stt_mic_source")

        /**
         * Which local checkpoint the on-device engine decodes with: either
         * [BUILTIN_GIGAAM_ID] or a [CustomSttModel.id][com.ai.assistance.operit.data.speech.CustomSttModel].
         * An id whose entry is gone falls back to built-in at use time; the
         * stored value is left alone so reinstalling the model just works.
         */
        private val LOCAL_STT_MODEL_ID = stringPreferencesKey("local_stt_model_id")

        const val BUILTIN_GIGAAM_ID = "gigaam"

        const val DEFAULT_TTS_SPEECH_RATE = 1.0f
        const val DEFAULT_TTS_PITCH = 1.0f

        // HTTP TTS的默认预设
        val DEFAULT_HTTP_TTS_PRESET = TtsHttpConfig(
            urlTemplate = "",
            apiKey = "",
            headers = emptyMap(),
            httpMethod = "GET",
            requestBody = "",
            contentType = "application/json",
            localeTag = "",
            voiceId = "",
            modelName = "",
            responsePipeline = emptyList()
        )

        val DEFAULT_VITS_TTS_PACKAGE_CONFIG = VitsTtsPackageConfig()

        val DEFAULT_STT_HTTP_PRESET = SttHttpConfig(
            endpointUrl = "https://api.openai.com/v1/audio/transcriptions",
            apiKey = "",
            modelName = "whisper-1",
        )

        // TTS Cleaner 的默认正则表达式列表（去除中英文括号内容）
        val DEFAULT_TTS_CLEANER_REGEXS = listOf(
            "\\([^)]+\\)",  // 英文括号
            "（[^）]+）"     // 中文括号
        )

        private fun parseSttServiceType(raw: String?): SpeechServiceFactory.SpeechServiceType {
            if (raw == null) return DEFAULT_STT_SERVICE_TYPE
            // Removed engines resolve to the offline replacement, so an upgrade
            // never lands on a dead engine or an unexpected remote one.
            if (raw == "SHERPA_MNN" || raw == "SHERPA_NCNN") {
                return SpeechServiceFactory.SpeechServiceType.LOCAL_GIGAAM
            }
            return runCatching { SpeechServiceFactory.SpeechServiceType.valueOf(raw) }
                .getOrElse { DEFAULT_STT_SERVICE_TYPE }
        }
    }

    // --- TTS Flows ---
    val ttsServiceTypeFlow: Flow<VoiceServiceFactory.VoiceServiceType> = dataStore.data.map { prefs ->
        // Fork 10.10: Chinese TTS types removed; stored names map to the default
        // instead of throwing inside valueOf.
        runCatching {
            VoiceServiceFactory.VoiceServiceType.valueOf(
                prefs[TTS_SERVICE_TYPE] ?: DEFAULT_TTS_SERVICE_TYPE.name
            )
        }.getOrDefault(DEFAULT_TTS_SERVICE_TYPE)
    }

    val ttsHttpConfigFlow: Flow<TtsHttpConfig> = dataStore.data.map { prefs ->
        val json = prefs[TTS_HTTP_CONFIG]
        if (json != null) {
            try {
                serializerJson.decodeFromString<TtsHttpConfig>(json)
            } catch (e: Exception) {
                DEFAULT_HTTP_TTS_PRESET // Fallback to default preset on parsing error
            }
        } else {
            DEFAULT_HTTP_TTS_PRESET
        }
    }

    val ttsVitsPackageConfigFlow: Flow<VitsTtsPackageConfig> = dataStore.data.map { prefs ->
        val json = prefs[TTS_VITS_PACKAGE_CONFIG]
        if (json == null) {
            DEFAULT_VITS_TTS_PACKAGE_CONFIG
        } else {
            serializerJson.decodeFromString<VitsTtsPackageConfig>(json)
        }
    }

    val ttsCleanerRegexsFlow: Flow<List<String>> = dataStore.data.map { prefs ->
        val storedRegexs = prefs[TTS_CLEANER_REGEXS]
        if (storedRegexs == null) {
            DEFAULT_TTS_CLEANER_REGEXS
        } else {
            storedRegexs.toList()
        }
    }

    val ttsSpeechRateFlow: Flow<Float> = dataStore.data.map { prefs ->
        prefs[TTS_SPEECH_RATE] ?: DEFAULT_TTS_SPEECH_RATE
    }

    val ttsPitchFlow: Flow<Float> = dataStore.data.map { prefs ->
        prefs[TTS_PITCH] ?: DEFAULT_TTS_PITCH
    }

    // --- STT Flows ---
    val sttServiceTypeFlow: Flow<SpeechServiceFactory.SpeechServiceType> = dataStore.data.map { prefs ->
        parseSttServiceType(prefs[STT_SERVICE_TYPE])
    }

    val sttHttpConfigFlow: Flow<SttHttpConfig> = dataStore.data.map { prefs ->
        val json = prefs[STT_HTTP_CONFIG]
        if (json != null) {
            try {
                serializerJson.decodeFromString<SttHttpConfig>(json)
            } catch (e: Exception) {
                DEFAULT_STT_HTTP_PRESET
            }
        } else {
            DEFAULT_STT_HTTP_PRESET
        }
    }

    // --- Save TTS Settings ---
    suspend fun saveTtsSettings(
        serviceType: VoiceServiceFactory.VoiceServiceType,
        httpConfig: TtsHttpConfig? = null,
        vitsConfig: VitsTtsPackageConfig? = null,
        cleanerRegexs: List<String>? = null,
        speechRate: Float? = null,
        pitch: Float? = null
    ) {
        dataStore.edit { prefs ->
            prefs[TTS_SERVICE_TYPE] = serviceType.name

            // 系统 TTS 也从这份旧字段读取语言和音色，迁移投影必须保留它们。
            httpConfig?.let { prefs[TTS_HTTP_CONFIG] = serializerJson.encodeToString(it) }

            cleanerRegexs?.let {
                prefs[TTS_CLEANER_REGEXS] = it.filter { regex -> regex.isNotBlank() }.toSet()
            }

            speechRate?.let { prefs[TTS_SPEECH_RATE] = it }
            pitch?.let { prefs[TTS_PITCH] = it }

            if (serviceType == VoiceServiceFactory.VoiceServiceType.VITS_TTS) {
                vitsConfig?.let { prefs[TTS_VITS_PACKAGE_CONFIG] = serializerJson.encodeToString(it) }
            }
        }
    }

    /** 只保存 TTS 清理正则列表 */
    suspend fun saveTtsCleanerRegexs(regexs: List<String>) {
        dataStore.edit { prefs ->
            prefs[TTS_CLEANER_REGEXS] = regexs.filter { it.isNotBlank() }.toSet()
        }
    }

    // --- Local STT tuning ---
    val localSttTuningFlow: Flow<LocalSttTuning> = dataStore.data.map { prefs ->
        LocalSttTuning(
            vadEnabled = prefs[STT_VAD_ENABLED] ?: DEFAULT_LOCAL_STT_TUNING.vadEnabled,
            vadAggressive = prefs[STT_VAD_AGGRESSIVE] ?: DEFAULT_LOCAL_STT_TUNING.vadAggressive,
            endpointSilenceMs =
                (prefs[STT_ENDPOINT_SILENCE_MS] ?: DEFAULT_LOCAL_STT_TUNING.endpointSilenceMs)
                    .takeIf { it in LocalSttTuning.ENDPOINT_OPTIONS_MS }
                    ?: DEFAULT_LOCAL_STT_TUNING.endpointSilenceMs,
            micSource = prefs[STT_MIC_SOURCE] ?: DEFAULT_LOCAL_STT_TUNING.micSource,
        )
    }

    suspend fun saveLocalSttTuning(tuning: LocalSttTuning) {
        dataStore.edit { prefs ->
            prefs[STT_VAD_ENABLED] = tuning.vadEnabled
            prefs[STT_VAD_AGGRESSIVE] = tuning.vadAggressive
            prefs[STT_ENDPOINT_SILENCE_MS] = tuning.endpointSilenceMs
            prefs[STT_MIC_SOURCE] = tuning.micSource
        }
    }

    // --- Local STT checkpoint ---
    val localSttModelIdFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[LOCAL_STT_MODEL_ID] ?: BUILTIN_GIGAAM_ID
    }

    suspend fun saveLocalSttModelId(id: String) {
        dataStore.edit { prefs ->
            prefs[LOCAL_STT_MODEL_ID] = id.ifEmpty { BUILTIN_GIGAAM_ID }
        }
    }

    // --- Save STT Settings ---
    suspend fun saveSttSettings(
        serviceType: SpeechServiceFactory.SpeechServiceType,
        httpConfig: SttHttpConfig? = null,
    ) {
        dataStore.edit { prefs ->
            prefs[STT_SERVICE_TYPE] = serviceType.name

            when (serviceType) {
                SpeechServiceFactory.SpeechServiceType.LOCAL_GIGAAM -> {
                }
                SpeechServiceFactory.SpeechServiceType.OPENAI_STT -> {
                    httpConfig?.let { prefs[STT_HTTP_CONFIG] = serializerJson.encodeToString(it) }
                }
                SpeechServiceFactory.SpeechServiceType.DEEPGRAM_STT -> {
                    httpConfig?.let { prefs[STT_HTTP_CONFIG] = serializerJson.encodeToString(it) }
                }
            }
        }
    }
}
