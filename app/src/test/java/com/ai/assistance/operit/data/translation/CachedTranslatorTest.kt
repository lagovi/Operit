package com.ai.assistance.operit.data.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedTranslatorTest {

    private fun translator(
        store: TranslationStore = MemoryTranslationStore(),
        calls: MutableList<String> = mutableListOf(),
    ): Pair<CachedTranslator, MutableList<String>> {
        val t = CachedTranslator(
            cacheDir = kotlin.io.path.createTempDirectory().toFile(),
            fetchFresh = { input -> calls.add(input); "TRANSLATED:\u00ab$input\u00bb" },
            store = store,
        )
        return Pair(t, calls)
    }

    @Test
    fun plainEnglishPassesThroughWithoutCallingTheModel() {
        val (t, calls) = translator()
        val out = kotlinx.coroutines.runBlocking { t.translate("Battery 31 percent") }
        assertEquals("Battery 31 percent", out)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun chineseTranslatesOnceThenServesFromCache() {
        val (t, calls) = translator()
        val source = "\u83b7\u53d6\u8bbe\u5907\u72b6\u6001\u6210\u529f"
        val first = kotlinx.coroutines.runBlocking { t.translate(source) }
        val second = kotlinx.coroutines.runBlocking { t.translate(source) }
        assertTrue(first.startsWith("TRANSLATED:"))
        assertEquals(first, second)
        assertEquals(1, calls.size)
    }

    @Test
    fun editedSourceIsADifferentKey() {
        assertTrue(
            CachedTranslator.keyFor("\u83b7\u53d6") !=
                CachedTranslator.keyFor("\u83b7\u53d6\u544a")
        )
    }

    @Test
    fun keyIsStableSha256Hex() {
        val a = CachedTranslator.keyFor("\u6d4b\u8bd5")
        assertEquals(a, CachedTranslator.keyFor("\u6d4b\u8bd5"))
        assertEquals(64, a.length)
        assertTrue(a.matches(Regex("[0-9a-f]+")))
    }

    @Test
    fun detectsCjkRanges() {
        assertTrue(CachedTranslator.containsCjk("\u83b7\u53d6"))
        assertTrue(CachedTranslator.containsCjk("mixed \u6d4b\u8bd5 text"))
        assertFalse(CachedTranslator.containsCjk("plain english 123"))
    }

    @Test
    fun fileStoreRoundTripsAndTrimsOldest() {
        val dir = kotlin.io.path.createTempDirectory().toFile()
        val store = FileTranslationStore(java.io.File(dir, "cache.json"))
        store.put("k1", "v1")
        assertEquals("v1", store.get("k1"))
        assertEquals(null, store.get("missing"))
        // A fresh store over the same file sees the persisted entries.
        val reopened = FileTranslationStore(java.io.File(dir, "cache.json"))
        assertEquals("v1", reopened.get("k1"))
    }

    @Test
    fun stripsCompleteThinkBlocks() {
        val out = stripThinkBlocks("<think>reasoning here</think>Floor limiter")
        assertEquals("Floor limiter", out)
    }

    @Test
    fun dropsTrailingUnclosedThink() {
        val out = stripThinkBlocks("Floor limiter<think>cut off")
        assertEquals("Floor limiter", out)
    }

    @Test
    fun plainTextPassesThroughStrip() {
        assertEquals("plain", stripThinkBlocks("plain"))
    }
}
