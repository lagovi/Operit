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
        peek(text)?.let { return it }
        val fresh = fetchFresh(text).trim()
        putTranslation(text, fresh)
        return fresh.ifEmpty { text }
    }

    /**
     * Synchronous cache read: null on non-CJK or a miss. Composables use it
     * as the initial value so a cached translation shows on the first frame
     * instead of flashing the source until the lookup coroutine runs.
     */
    fun peek(text: String): String? {
        if (!containsCjk(text)) return null
        return store.get(keyFor(text))
    }

    /**
     * Stores one translation under the source-hash key. Empty results and
     * echo answers are dropped: the caller shows the source instead.
     */
    fun putTranslation(source: String, translation: String) {
        val fresh = translation.trim()
        if (fresh.isNotEmpty() && fresh != source) {
            store.put(keyFor(source), fresh)
        }
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

private const val CACHE_PATH = "translation_cache/v2.json"

/**
 * Parses a batch translation response: a JSON object keyed by 1-based index,
 * as requested by FunctionalPrompts.translationBatchUserPrompt. The raw
 * stream may carry <think> blocks or prose around the object; the outermost
 * braces are extracted first. Unparseable or empty entries are dropped, the
 * caller shows those sources untranslated.
 *
 * @return 0-based index to translation.
 */
fun parseBatchTranslations(raw: String): Map<Int, String> {
    val stripped = stripThinkBlocks(raw)
    val start = stripped.indexOf('{')
    val end = stripped.lastIndexOf('}')
    if (start < 0 || end <= start) return emptyMap()
    val out = LinkedHashMap<Int, String>()
    try {
        val json = org.json.JSONObject(stripped.substring(start, end + 1))
        for (key in json.keys()) {
            val index = key.toIntOrNull()?.minus(1) ?: continue
            if (index < 0) continue
            json.optString(key).trim().takeIf { it.isNotEmpty() }?.let {
                out[index] = it
            }
        }
    } catch (e: Exception) {
        AppLogger.e(TAG_BATCH, "Cannot parse batch translation", e)
    }
    return out
}

private const val TAG_BATCH = "BatchTranslations"

/**
 * Removes model thinking blocks from a translation response.
 *
 * Reasoning models wrap their work in <think> tags and the translation
 * caller collects the raw stream, so without this the cached "translation"
 * is pages of thinking. Complete blocks are cut; a trailing unclosed tag
 * (cut stream) drops everything from it to the end. Callers keep the raw
 * text when nothing remains, since a missing answer beats a wrong edit.
 */
fun stripThinkBlocks(text: String): String {
    var out = THINK_BLOCK.replace(text, "")
    val open = out.lastIndexOf("<think>")
    if (open >= 0) {
        out = out.substring(0, open)
    }
    return out.trim()
}

private val THINK_BLOCK = Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL)
