package com.ai.assistance.operit.data.translation

import com.ai.assistance.operit.util.AppLogger
import java.io.File
import java.security.MessageDigest

/**
 * Translations of runtime Chinese by the default LLM, cached so no string
 * is ever translated twice.
 *
 * Static UI strings are English by packaging; what remains is dynamic text
 * the app cannot ship translated: tool result messages, plugin metadata
 * from the market, tool error prefixes. Those go through here.
 *
 * Update detection is by hash: the cache key is the SHA-256 of the source
 * text, so any upstream edit is a different key and re-translates on
 * demand. Entries are capped ([MAX_ENTRIES]); beyond that the oldest go,
 * because an unbounded cache in filesDir is a slow storage leak.
 *
 * @param fetchFresh fresh translation of a cache miss, e.g. the chat service.
 *   Failures propagate to the caller, which decides what the user sees.
 */
class CachedTranslator(
    cacheDir: File,
    private val fetchFresh: suspend (String) -> String,
    private val store: TranslationStore = FileTranslationStore(
        File(cacheDir, CACHE_PATH)
    ),
) {
    /**
     * @return the input untouched when it holds no CJK, the cached
     *   translation on a hit, or a fresh translation that is then stored.
     */
    suspend fun translate(text: String): String {
        if (!containsCjk(text)) return text
        val key = keyFor(text)
        store.get(key)?.let { return it }
        val fresh = fetchFresh(text).trim()
        if (fresh.isNotEmpty() && fresh != text) {
            store.put(key, fresh)
        }
        return fresh.ifEmpty { text }
    }

    companion object {
        internal const val MAX_ENTRIES = 2000

        /** True when there is anything here a translation could change. */
        fun containsCjk(text: String): Boolean = CJK_REGEX.containsMatchIn(text)

        /** Cache key: the source hash, so edits re-translate by construction. */
        fun keyFor(text: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(text.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }

        private val CJK_REGEX = Regex("[\u4E00-\u9FFF\u3400-\u4DBF\uF900-\uFAFF\u3040-\u30FF\uAC00-\uD7AF]")
    }
}

interface TranslationStore {
    fun get(key: String): String?
    fun put(key: String, translation: String)
}

/** In-memory store: tests, and callers that do not want disk. */
class MemoryTranslationStore : TranslationStore {
    private val entries = LinkedHashMap<String, String>()
    override fun get(key: String): String? = entries[key]
    override fun put(key: String, translation: String) {
        entries[key] = translation
    }
    fun size(): Int = entries.size
}

/** JSON file store, insertion-ordered so trimming drops the oldest. */
class FileTranslationStore(private val file: File) : TranslationStore {
    private val lock = Any()
    private var entries: LinkedHashMap<String, String>? = null

    override fun get(key: String): String? = synchronized(lock) {
        loadLocked()[key]
    }

    override fun put(key: String, translation: String) = synchronized(lock) {
        val map = loadLocked()
        map[key] = translation
        while (map.size > CachedTranslator.MAX_ENTRIES) {
            val oldest = map.keys.first()
            map.remove(oldest)
        }
        saveLocked(map)
    }

    private fun loadLocked(): LinkedHashMap<String, String> {
        entries?.let { return it }
        val map = LinkedHashMap<String, String>()
        if (file.isFile) {
            try {
                val json = org.json.JSONObject(file.readText())
                for (key in json.keys()) {
                    json.optString(key).takeIf { it.isNotEmpty() }?.let {
                        map[key] = it
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Cannot read the translation cache, starting empty", e)
            }
        }
        entries = map
        return map
    }

    private fun saveLocked(map: LinkedHashMap<String, String>) {
        try {
            file.parentFile?.mkdirs()
            val json = org.json.JSONObject()
            for ((key, value) in map) json.put(key, value)
            file.writeText(json.toString())
        } catch (e: Exception) {
            AppLogger.e(TAG, "Cannot write the translation cache", e)
        }
    }

    private companion object {
        const val TAG = "FileTranslationStore"
    }
}

private const val CACHE_PATH = "translation_cache/v1.json"
