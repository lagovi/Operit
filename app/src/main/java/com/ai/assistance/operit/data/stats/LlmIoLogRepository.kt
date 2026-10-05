package com.ai.assistance.operit.data.stats

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.ai.assistance.operit.core.chat.hooks.PromptTurn
import com.ai.assistance.operit.data.dao.LlmIoLogDao
import com.ai.assistance.operit.data.db.AppDatabase
import com.ai.assistance.operit.data.model.LlmIoLogEntity
import com.ai.assistance.operit.data.model.ModelParameter
import com.ai.assistance.operit.util.AppLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

private val Context.llmIoLogDataStore by
    preferencesDataStore(name = "llm_io_log_preferences")

/**
 * Room owner for the LLM I/O log: full request/response bodies of formal
 * inference calls. Writes happen on Dispatchers.IO after the response stream
 * has been fully delivered, so logging never blocks the chat.
 */
class LlmIoLogRepository private constructor(context: Context) {
    companion object {
        private const val TAG = "LlmIoLogRepository"

        /** Function label for plain chat and for services without a function tag. */
        const val FUNCTION_CHAT = "CHAT"

        /** Function label for ad-hoc custom-config services. */
        const val FUNCTION_AD_HOC = "AD_HOC"

        /** Row cap: newest entries are kept, older ones pruned on insert. */
        const val MAX_ENTRIES = 500

        /** Byte cap on request+response bodies (LENGTH sum over TEXT columns). */
        const val MAX_BYTES = 8L * 1024L * 1024L

        /** Per-turn content cap before the JSON is built. */
        const val MAX_TURN_CHARS = 8_000

        /** Whole request JSON cap. */
        const val MAX_REQUEST_CHARS = 65_536

        /** Collected response cap. */
        const val MAX_RESPONSE_CHARS = 262_144

        const val TRUNCATION_MARKER = "[truncated]"

        private val ENABLED_KEY = booleanPreferencesKey("enabled")

        @Volatile
        private var instance: LlmIoLogRepository? = null
        private val databaseAccessMutex = Mutex()

        fun getInstance(context: Context): LlmIoLogRepository =
            instance ?: synchronized(this) {
                instance ?: LlmIoLogRepository(context.applicationContext).also { instance = it }
            }

        /** Prevent Room access while a restore replaces database files. */
        suspend fun <T> withDatabaseAccess(block: suspend () -> T): T =
            databaseAccessMutex.withLock { block() }

        suspend fun <T> withDatabaseRestore(block: suspend () -> T): T =
            withDatabaseAccess { block() }

        fun truncateMiddle(value: String, maxChars: Int): String {
            if (value.length <= maxChars) return value
            return value.substring(0, maxChars) + TRUNCATION_MARKER
        }

        /**
         * Serializes one request for the log. Turn contents are capped
         * individually, tool metadata is reduced to tool names, model
         * parameters to enabled apiName=value pairs. No secrets pass through
         * here: API keys travel in HTTP headers, never in turns or params.
         */
        fun formatRequest(
            turns: List<PromptTurn>,
            parameters: List<ModelParameter<*>>,
            enableThinking: Boolean,
            stream: Boolean,
        ): String {
            val turnsArray = JSONArray()
            for (turn in turns) {
                val entry = JSONObject()
                entry.put("role", turn.role)
                entry.put("content", truncateMiddle(turn.content, MAX_TURN_CHARS))
                turn.toolName?.let { entry.put("tool", it) }
                turnsArray.put(entry)
            }
            val paramsArray = JSONArray()
            for (param in parameters) {
                if (!param.isEnabled) continue
                val entry = JSONObject()
                entry.put("name", param.apiName)
                entry.put("value", truncateMiddle(param.currentValue.toString(), 256))
                paramsArray.put(entry)
            }
            val root = JSONObject()
            root.put("turns", turnsArray)
            root.put("parameters", paramsArray)
            root.put("thinking", enableThinking)
            root.put("stream", stream)
            return truncateMiddle(root.toString(), MAX_REQUEST_CHARS)
        }

        /** Derives prompt/completion counts from a finished token-usage record. */
        fun promptTokensOf(
            uncachedInputTokens: Long?,
            cachedInputTokens: Long?,
            cacheWriteTokens: Long?,
            totalInputTokens: Long?,
        ): Long? {
            totalInputTokens?.let { return it }
            var sum = 0L
            var known = false
            for (part in listOf(uncachedInputTokens, cachedInputTokens, cacheWriteTokens)) {
                if (part != null) {
                    known = true
                    sum = if (Long.MAX_VALUE - sum < part) Long.MAX_VALUE else sum + part
                }
            }
            return if (known) sum else null
        }
    }

    private val appContext = context.applicationContext
    private val dataStore = appContext.llmIoLogDataStore

    @Volatile
    private var enabledCache: Boolean? = null

    suspend fun isEnabled(): Boolean {
        enabledCache?.let { return it }
        val enabled = dataStore.data.map { it[ENABLED_KEY] ?: true }.first()
        enabledCache = enabled
        return enabled
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[ENABLED_KEY] = enabled }
        enabledCache = enabled
    }

    internal suspend fun <T> withDao(block: suspend (LlmIoLogDao) -> T): T {
        return withDatabaseAccess { block(AppDatabase.getDatabase(appContext).llmIoLogDao()) }
    }

    suspend fun record(record: LlmIoLogEntity) {
        withDao { dao ->
            dao.insert(record)
            pruneLocked(dao)
        }
    }

    private suspend fun pruneLocked(dao: LlmIoLogDao) {
        dao.pruneToNewest(MAX_ENTRIES)
        var guard = 0
        while (dao.totalChars() > MAX_BYTES && guard < 64) {
            val deleted = dao.deleteOldest(32)
            guard += 1
            if (deleted <= 0) break
        }
        if (guard >= 64) {
            AppLogger.w(TAG, "byte-cap prune hit iteration guard")
        }
    }

    suspend fun recent(
        modelId: String?,
        limit: Int,
        offset: Int,
    ): List<LlmIoLogEntity> = withDao { dao -> dao.recent(modelId, limit, offset) }

    suspend fun models(): List<String> = withDao { dao -> dao.distinctModels() }

    suspend fun get(id: Long): LlmIoLogEntity? = withDao { dao -> dao.get(id) }

    suspend fun delete(id: Long): Int = withDao { dao -> dao.delete(id) }

    suspend fun clear() {
        withDao { dao -> dao.deleteAll() }
    }

    suspend fun count(): Long = withDao { dao -> dao.count() }
}
