package com.ai.assistance.operit.util

import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Text segmentation for memory keyword expansion.
 *
 * Fork: the previous implementation ran a Chinese dictionary segmenter
 * (com.huaban:jieba-analysis). Two things made that the wrong tool here. It
 * shipped ~2.4 MB of Chinese word dictionaries that an English build cannot use,
 * and applied to Latin text a Chinese dictionary yields character bigrams, so
 * memory search was matching on fragments rather than words.
 *
 * Segmentation is now by word boundary, which is what keyword expansion for
 * Latin-script queries actually wants. The API is unchanged, so callers do not
 * need to know which implementation backs it.
 */
object TextSegmenter {
    private const val TAG = "TextSegmenter"

    private val WORD_BOUNDARY =
            Regex("[\\s\\p{Punct}\\p{IsHan}]+")

    /** Single letters and short fragments are noise for keyword expansion. */
    private const val MIN_TOKEN_LENGTH = 2

    // Keyword cache, to keep repeated searches over the same text cheap.
    private val segmentCache = ConcurrentHashMap<String, List<String>>()

    private const val MAX_CACHE_SIZE = 1000

    /**
     * Initialize the segmenter.
     *
     * Kept for the call site that warms this up during app start so that the
     * first memory search does not pay for the setup. There is no dictionary to
     * load any more, so this only primes the cache machinery.
     */
    fun initialize(context: android.content.Context, customDictPath: String? = null) {
        if (customDictPath != null) {
            AppLogger.d(
                TAG,
                "Custom dictionary '$customDictPath' is not used: segmentation is word-boundary based"
            )
        }
        segmentCache.clear()
    }

    /**
     * Split [text] into keyword tokens.
     *
     * @param text text to segment
     * @param useCached whether to consult and populate the segment cache
     * @return the tokens, in order of appearance, without duplicates
     */
    fun segment(text: String, useCached: Boolean = true): List<String> {
        if (text.isBlank()) return emptyList()

        if (useCached) {
            segmentCache[text]?.let { return it }
        }

        val tokens =
                text.lowercase(Locale.ROOT)
                        .split(WORD_BOUNDARY)
                        .map { it.trim() }
                        .filter { it.length >= MIN_TOKEN_LENGTH }
                        .distinct()

        if (useCached) {
            if (segmentCache.size >= MAX_CACHE_SIZE) {
                segmentCache.keys.take(MAX_CACHE_SIZE / 2).forEach { segmentCache.remove(it) }
            }
            segmentCache[text] = tokens
        }

        return tokens
    }

    /** Drop the segment cache. */
    fun clearCache() {
        segmentCache.clear()
    }

    /**
     * Score how relevant [keywords] are to [text].
     *
     * Exact substring matches are scored first, because a token that appears
     * verbatim is stronger evidence than one that only shares a segment.
     *
     * @param text text to score against
     * @param keywords keywords to look for
     * @return a score in 0..1
     */
    fun calculateRelevance(text: String, keywords: List<String>): Double {
        if (text.isBlank() || keywords.isEmpty()) return 0.0

        // Ignore very long text: it cannot add signal and costs time to scan.
        val maxLength = 5000
        val textToProcess = if (text.length > maxLength) text.substring(0, maxLength) else text
        val textLower = textToProcess.lowercase()

        val normalizedKeywords =
                keywords.map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }
        if (normalizedKeywords.isEmpty()) return 0.0

        val exactMatches = normalizedKeywords.count { keyword ->
            textLower.contains(keyword)
        }
        if (exactMatches == 0) {
            return 0.0
        }

        if (exactMatches >= normalizedKeywords.size / 2 || exactMatches >= 3) {
            val quickScore = (exactMatches * 2.0) / (normalizedKeywords.size * 3.0)
            return quickScore.coerceIn(0.0, 1.0)
        }

        val textSegments = segment(textLower).toSet()
        val segmentMatches = normalizedKeywords.count { keyword ->
            textSegments.any { segment -> segment.contains(keyword) }
        }

        val totalScore =
                (exactMatches * 2.0 + segmentMatches) / (normalizedKeywords.size * 3.0)
        return totalScore.coerceIn(0.0, 1.0)
    }
}
