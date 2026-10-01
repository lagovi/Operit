package com.ai.assistance.operit.api.speech

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Greedy CTC decoding for the GigaAM-Multilingual vocabulary.
 *
 * The vocabulary is character-wise and its layout is not the usual one. In
 * `tokens.txt`:
 *
 *  - index 0 is the **space**, not a blank;
 *  - the last index is `<blk>`, the CTC blank.
 *
 * So the decode is "argmax per frame, skip `<blk>`, collapse runs of equal
 * ids". Writing `if (id == 0) continue` — the reflex from SentencePiece
 * vocabularies — yields text with no spaces at all.
 *
 * The alphabet has no digits, no uppercase and no punctuation beyond the
 * apostrophe. That is a property of the checkpoint, not of this code: dictated
 * digits do not come out as digits.
 */
class GigaAMTokenVocabulary private constructor(private val tokens: Array<String>) {

    val blankId: Int get() = tokens.size - 1

    val size: Int get() = tokens.size

    fun symbol(id: Int): String = tokens[id]

    /**
     * @param logProbs `[frames, vocabSize]` — use [get] on the transposed view
     *   only if the row stride matches; prefer passing per-frame rows.
     */
    fun greedyDecode(logProbs: FloatArray, frameCount: Int, vocabSize: Int): String {
        require(frameCount >= 0) { "frameCount must not be negative: $frameCount" }
        require(vocabSize == tokens.size) {
            "logits carry $vocabSize classes but the vocabulary has ${tokens.size}"
        }
        val sb = StringBuilder()
        var previous = -1
        for (f in 0 until frameCount) {
            val row = f * vocabSize
            var best = 0
            var bestValue = Float.NEGATIVE_INFINITY
            for (v in 0 until vocabSize) {
                val value = logProbs[row + v]
                if (value > bestValue) {
                    bestValue = value
                    best = v
                }
            }
            if (best != previous && best != blankId) {
                sb.append(tokens[best])
            }
            previous = best
        }
        return sb.toString()
    }

    companion object {
        /**
         * Parses `tokens.txt`, whose lines are `<symbol> <id>`. Lines are read in
         * file order, which is already id order for this export; the parsed ids
         * are asserted to agree so a reordered file cannot be silently misread.
         */
        fun parse(stream: InputStream): GigaAMTokenVocabulary {
            val parsed = ArrayList<Pair<Int, String>>()
            BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    val trimmed = line.trimEnd('\n', '\r')
                    if (trimmed.isNotEmpty()) {
                        val split = trimmed.lastIndexOf(' ')
                        require(split > 0) { "malformed tokens.txt line: $trimmed" }
                        val symbol = trimmed.substring(0, split)
                        val id = trimmed.substring(split + 1).trim().toInt()
                        parsed.add(id to symbol)
                    }
                    line = reader.readLine()
                }
            }
            require(parsed.isNotEmpty()) { "tokens.txt is empty" }
            val symbols = arrayOfNulls<String>(parsed.size)
            for ((id, symbol) in parsed) {
                require(id in symbols.indices) { "tokens.txt id $id is out of range" }
                symbols[id] = symbol
            }
            val missing = symbols.indices.filter { symbols[it] == null }
            require(missing.isEmpty()) { "tokens.txt is missing ids $missing" }
            @Suppress("UNCHECKED_CAST")
            return GigaAMTokenVocabulary(symbols as Array<String>)
        }
    }
}
