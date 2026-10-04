package com.ai.assistance.operit.data.translation

import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates batch prefetch with per-item translation.
 *
 * A screen prefetches its strings ([prefetchRuntimeTranslations]) while each
 * [TranslatedText] also resolves its own string on composition. Without
 * coordination every uncached string would be requested twice: once in the
 * batch and once individually. The prefetcher marks its in-flight keys here;
 * composables whose key is marked wait for the batch (bounded) instead of
 * firing a duplicate individual request, then read the warmed cache.
 */
object TranslationPrefetch {
    private val active = ConcurrentHashMap<String, Boolean>()

    fun mark(sources: Collection<String>) {
        for (source in sources) active[CachedTranslator.keyFor(source)] = true
    }

    fun unmark(sources: Collection<String>) {
        for (source in sources) active.remove(CachedTranslator.keyFor(source))
    }

    fun isActive(key: String): Boolean = active.containsKey(key)
}
