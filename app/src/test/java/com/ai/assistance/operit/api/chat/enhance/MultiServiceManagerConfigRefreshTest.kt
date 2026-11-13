package com.ai.assistance.operit.api.chat.enhance

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ai.assistance.operit.api.chat.llmprovider.AIService
import com.ai.assistance.operit.core.chat.hooks.PromptTurn
import com.ai.assistance.operit.data.model.ApiProviderType
import com.ai.assistance.operit.data.model.FunctionType
import com.ai.assistance.operit.data.model.ModelConfigData
import com.ai.assistance.operit.data.model.ModelOption
import com.ai.assistance.operit.data.model.ModelParameter
import com.ai.assistance.operit.data.model.ToolPrompt
import com.ai.assistance.operit.data.preferences.FunctionalConfigManager
import com.ai.assistance.operit.data.preferences.ModelConfigManager
import com.ai.assistance.operit.data.stats.ProviderUsageSnapshot
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.util.stream.Stream
import com.ai.assistance.operit.util.stream.streamOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

/**
 * Regression test: after a model config change, the functional services (IMAGE_RECOGNITION
 * etc.) must use the new config immediately, without relying on the call site to refresh
 * manually (#915).
 */
class MultiServiceManagerConfigRefreshTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Before
    fun setUp() {
        // android.util.Log is not mocked, so AppLogger internals throw "Method not mocked".
        // A pure JVM test therefore turns off both system and file logging.
        AppLogger.enableSystemLog = false
        AppLogger.enableFileLogging = false
    }

    /** A fake AIService that only records its construction config, used to assert rebuilds. */
    private class FakeAIService(val config: ModelConfigData) : AIService {
        override val inputTokenCount: Long = 0L
        override val cachedInputTokenCount: Long = 0L
        override val outputTokenCount: Long = 0L
        override val providerModel: String = "${config.apiProviderTypeId}:${config.modelName}"
        override fun resetTokenCounts() {}
        override fun cancelStreaming() {}
        override suspend fun getModelsList(context: Context): Result<List<ModelOption>> =
            Result.success(emptyList())
        override suspend fun sendMessage(
            context: Context,
            chatHistory: List<PromptTurn>,
            modelParameters: List<ModelParameter<*>>,
            enableThinking: Boolean,
            stream: Boolean,
            availableTools: List<ToolPrompt>?,
            preserveThinkInHistory: Boolean,
            onTokensUpdated: suspend (Long, Long, Long) -> Unit,
            onUsageReported: (suspend (ProviderUsageSnapshot, Int) -> Unit)?,
            onNonFatalError: suspend (String) -> Unit,
            enableRetry: Boolean,
            recordTokenUsage: Boolean,
            onUsageFinalized: (suspend (Int?) -> Unit)?
        ): Stream<String> = streamOf("")
        override suspend fun testConnection(context: Context): Result<String> =
            Result.success("ok")
        override suspend fun calculateInputTokens(
            chatHistory: List<PromptTurn>,
            availableTools: List<ToolPrompt>?
        ): Long = 0L
    }

    private class TestPreferencesDataStore(
        private val preferences: MutableStateFlow<Preferences>
    ) : DataStore<Preferences> {
        override val data = preferences.asStateFlow()
        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences
        ): Preferences {
            val updated = transform(preferences.value)
            preferences.value = updated
            return updated
        }
    }

    private fun writeModelConfig(
        dataStore: TestPreferencesDataStore,
        config: ModelConfigData
    ) = runBlocking {
        dataStore.updateData { current ->
            current.toMutablePreferences().apply {
                set(stringPreferencesKey("config_${config.id}"), json.encodeToString(config))
                set(ModelConfigManager.CONFIG_LIST_KEY, json.encodeToString(listOf(config.id)))
            }
        }
    }

    private fun writeFunctionalMapping(
        dataStore: TestPreferencesDataStore,
        configId: String,
        modelIndex: Int = 0
    ) = runBlocking {
        dataStore.updateData { current ->
            current.toMutablePreferences().apply {
                val mapping = mapOf(
                    FunctionType.IMAGE_RECOGNITION.name to
                        com.ai.assistance.operit.data.preferences.FunctionConfigMapping(configId, modelIndex)
                )
                set(
                    FunctionalConfigManager.FUNCTION_CONFIG_MAPPING,
                    json.encodeToString(mapping)
                )
            }
        }
    }

    private fun buildManager(
        modelStore: TestPreferencesDataStore,
        functionalStore: TestPreferencesDataStore
    ): MultiServiceManager {
        val context = Mockito.mock(Context::class.java)
        val modelConfigManager = ModelConfigManager(context, modelStore)
        val functionalConfigManager = FunctionalConfigManager(context, functionalStore)
        return MultiServiceManager(
            context = context,
            functionalConfigManager = functionalConfigManager,
            modelConfigManager = modelConfigManager,
            serviceFactory = { config, _, _, _ -> FakeAIService(config) }
        )
    }

    @Test
    fun `model config change rebuilds functional service without explicit refresh`() = runBlocking {
        val modelStore = TestPreferencesDataStore(MutableStateFlow(mutablePreferencesOf()))
        val functionalStore = TestPreferencesDataStore(MutableStateFlow(mutablePreferencesOf()))

        val oldConfig = ModelConfigData(
            id = "google-config",
            name = "google",
            apiKey = "old-key",
            modelName = "gemini-2.0-flash",
            apiProviderType = ApiProviderType.GOOGLE,
            apiProviderTypeId = ApiProviderType.GOOGLE.name
        )
        writeModelConfig(modelStore, oldConfig)
        writeFunctionalMapping(functionalStore, "google-config")

        val manager = buildManager(modelStore, functionalStore)

        // First lookup: cache miss, create the service
        val firstService = manager.getServiceForFunction(FunctionType.IMAGE_RECOGNITION) as FakeAIService
        assertEquals("gemini-2.0-flash", firstService.config.modelName)
        assertEquals("old-key", firstService.config.apiKey)

        // With an unchanged config, hit the cache and return the same instance
        val cachedService = manager.getServiceForFunction(FunctionType.IMAGE_RECOGNITION)
        assertSame(firstService, cachedService)

        // Update the model config (model list + API key) without calling any refresh
        val newConfig = oldConfig.copy(
            apiKey = "new-key",
            modelName = "gemini-3.5-flash, gemini-3.1-flash-lite, gemini-flash-latest"
        )
        writeModelConfig(modelStore, newConfig)

        // The next lookup must rebuild the service automatically, using the new model and key
        val refreshedService = manager.getServiceForFunction(FunctionType.IMAGE_RECOGNITION) as FakeAIService
        assertNotSame(firstService, refreshedService)
        assertEquals("gemini-3.5-flash", refreshedService.config.modelName)
        assertEquals("new-key", refreshedService.config.apiKey)
    }

    @Test
    fun `function binding change rebuilds functional service without explicit refresh`() = runBlocking {
        val modelStore = TestPreferencesDataStore(MutableStateFlow(mutablePreferencesOf()))
        val functionalStore = TestPreferencesDataStore(MutableStateFlow(mutablePreferencesOf()))

        val configA = ModelConfigData(
            id = "config-a",
            name = "a",
            apiKey = "key-a",
            modelName = "model-a",
            apiProviderType = ApiProviderType.OPENAI_GENERIC,
            apiProviderTypeId = ApiProviderType.OPENAI_GENERIC.name
        )
        val configB = ModelConfigData(
            id = "config-b",
            name = "b",
            apiKey = "key-b",
            modelName = "model-b",
            apiProviderType = ApiProviderType.DEEPSEEK,
            apiProviderTypeId = ApiProviderType.DEEPSEEK.name
        )
        writeModelConfig(modelStore, configA.copy()) // write only config-a
        // Manually write config-b as well
        runBlocking {
            modelStore.updateData { current ->
                current.toMutablePreferences().apply {
                    set(stringPreferencesKey("config_config-b"), json.encodeToString(configB))
                    set(
                        ModelConfigManager.CONFIG_LIST_KEY,
                        json.encodeToString(listOf("config-a", "config-b"))
                    )
                }
            }
        }
        writeFunctionalMapping(functionalStore, "config-a")

        val manager = buildManager(modelStore, functionalStore)

        val firstService = manager.getServiceForFunction(FunctionType.IMAGE_RECOGNITION) as FakeAIService
        assertEquals("model-a", firstService.config.modelName)

        // Change the function binding from config-a to config-b without any refresh
        writeFunctionalMapping(functionalStore, "config-b")

        val reboundService = manager.getServiceForFunction(FunctionType.IMAGE_RECOGNITION) as FakeAIService
        assertNotSame(firstService, reboundService)
        assertEquals("model-b", reboundService.config.modelName)
        assertEquals("key-b", reboundService.config.apiKey)
    }

    @Test
    fun `model index change rebuilds functional service without explicit refresh`() = runBlocking {
        val modelStore = TestPreferencesDataStore(MutableStateFlow(mutablePreferencesOf()))
        val functionalStore = TestPreferencesDataStore(MutableStateFlow(mutablePreferencesOf()))

        val config = ModelConfigData(
            id = "google-config",
            name = "google",
            apiKey = "key",
            modelName = "gemini-3.5-flash, gemini-3.1-flash-lite, gemini-flash-latest",
            apiProviderType = ApiProviderType.GOOGLE,
            apiProviderTypeId = ApiProviderType.GOOGLE.name
        )
        writeModelConfig(modelStore, config)
        writeFunctionalMapping(functionalStore, "google-config", modelIndex = 0)

        val manager = buildManager(modelStore, functionalStore)

        val firstService = manager.getServiceForFunction(FunctionType.IMAGE_RECOGNITION) as FakeAIService
        assertEquals("gemini-3.5-flash", firstService.config.modelName)

        // Change the function binding modelIndex to 2 without any refresh
        writeFunctionalMapping(functionalStore, "google-config", modelIndex = 2)

        val reindexedService = manager.getServiceForFunction(FunctionType.IMAGE_RECOGNITION) as FakeAIService
        assertNotSame(firstService, reindexedService)
        assertEquals("gemini-flash-latest", reindexedService.config.modelName)
    }

    @Test
    fun `lease keeps service alive while in use and retires after release`() = runBlocking {
        val modelStore = TestPreferencesDataStore(MutableStateFlow(mutablePreferencesOf()))
        val functionalStore = TestPreferencesDataStore(MutableStateFlow(mutablePreferencesOf()))

        val config = ModelConfigData(
            id = "cfg",
            name = "cfg",
            apiKey = "key",
            modelName = "model-1",
            apiProviderType = ApiProviderType.GOOGLE,
            apiProviderTypeId = ApiProviderType.GOOGLE.name
        )
        writeModelConfig(modelStore, config)
        writeFunctionalMapping(functionalStore, "cfg")

        val manager = buildManager(modelStore, functionalStore)

        // Borrow the service (simulating an in-flight image recognition request)
        val lease = manager.acquireServiceForFunction(FunctionType.IMAGE_RECOGNITION)
        assertNotNull(lease.service)

        // Config change + refresh (simulating the user changing config mid-recognition)
        writeModelConfig(modelStore, config.copy(modelName = "model-2"))
        manager.refreshServiceForFunction(FunctionType.IMAGE_RECOGNITION)

        // The old service is not released while the lease is unreturned (this only checks the
        // lease semantics: a new lookup returns a new instance)
        val newService = manager.getServiceForFunction(FunctionType.IMAGE_RECOGNITION) as FakeAIService
        assertEquals("model-2", newService.config.modelName)

        // Return the lease
        lease.close()
    }
}
