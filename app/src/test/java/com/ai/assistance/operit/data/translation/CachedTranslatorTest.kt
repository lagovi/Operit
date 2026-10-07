package com.ai.assistance.operit.data.translation

import com.ai.assistance.operit.util.AppLogger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CachedTranslatorTest {
    private var previousSystemLogEnabled = true
    private var previousFileLogEnabled = true

    @Before
    fun disableAndroidLogging() {
        previousSystemLogEnabled = AppLogger.enableSystemLog
        previousFileLogEnabled = AppLogger.enableFileLogging
        AppLogger.enableSystemLog = false
        AppLogger.enableFileLogging = false
    }

    @After
    fun restoreAndroidLogging() {
        AppLogger.enableSystemLog = previousSystemLogEnabled
        AppLogger.enableFileLogging = previousFileLogEnabled
    }

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

    @Test
    fun peekSeesStoredTranslationsWithoutFetching() {
        val (t, calls) = translator()
        val source = "\u697c\u5c42\u9650\u5236\u5668"
        assertEquals(null, t.peek(source))
        t.putTranslation(source, "Floor Limiter")
        assertEquals("Floor Limiter", t.peek(source))
        assertTrue(calls.isEmpty())
    }

    @Test
    fun peekIgnoresNonCjk() {
        val (t, _) = translator()
        assertEquals(null, t.peek("plain english"))
    }

    @Test
    fun putTranslationDropsEchoAndEmpty() {
        val (t, _) = translator()
        val source = "\u83b7\u53d6\u8bbe\u5907\u72b6\u6001\u6210\u529f"
        t.putTranslation(source, source)
        t.putTranslation(source, "   ")
        assertEquals(null, t.peek(source))
    }

    @Test
    fun parseBatchReadsIndexKeyedJson() {
        val out = parseBatchTranslations("{\"1\":\"Hello\",\"2\":\"Thank you\"}")
        assertEquals(mapOf(0 to "Hello", 1 to "Thank you"), out)
    }

    @Test
    fun parseBatchSkipsThinkBlocksAndProse() {
        val out = parseBatchTranslations(
            "<think>translating two items</think>Here you go: {\"1\":\"Floor Limiter\"} done"
        )
        assertEquals(mapOf(0 to "Floor Limiter"), out)
    }

    @Test
    fun parseBatchDropsGarbage() {
        assertTrue(parseBatchTranslations("no braces here").isEmpty())
        assertTrue(parseBatchTranslations("{\"a\":\"b\"}").isEmpty())
        assertTrue(parseBatchTranslations("{\"1\":\"\"}").isEmpty())
    }

    @Test
    fun prefetchMarkRoundTrip() {
        val key = CachedTranslator.keyFor("warmup probe")
        assertFalse(TranslationPrefetch.isActive(key))
        TranslationPrefetch.mark(listOf("warmup probe"))
        assertTrue(TranslationPrefetch.isActive(key))
        TranslationPrefetch.unmark(listOf("warmup probe"))
        assertFalse(TranslationPrefetch.isActive(key))
    }

    @Test
    fun googleFallbackParsesSentences() {
        val raw = "[[[\"Hola\",\"Hello\",null,null,1],[\", \",\", \",null,null,1],[\"mundo\",\"world\",null,null,1]],null,\"es\"]"
        assertEquals("Hola, mundo", GoogleTranslateFallback.parseResponse(raw))
    }

    @Test
    fun googleFallbackDropsBadShapes() {
        assertEquals(null, GoogleTranslateFallback.parseResponse("oops"))
        assertEquals(null, GoogleTranslateFallback.parseResponse("[]"))
        assertEquals(null, GoogleTranslateFallback.parseResponse("[[]]"))
        assertEquals(null, GoogleTranslateFallback.parseResponse("{\"1\":\"x\"}"))
        assertEquals(null, GoogleTranslateFallback.parseResponse("[[[\"\",\"\",null,null,1]]]"))
    }

    @Test
    fun googleFallbackBuildsKeylessUrl() {
        val url = GoogleTranslateFallback.requestUrl("test query", "en")
        assertTrue(url.startsWith("https://translate.googleapis.com/translate_a/single?"))
        assertTrue(url.contains("client=gtx"))
        assertTrue(url.contains("tl=en"))
        assertTrue(url.contains("q=test+query"))
    }
}
