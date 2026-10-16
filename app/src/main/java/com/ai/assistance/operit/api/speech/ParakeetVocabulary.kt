package com.ai.assistance.operit.api.speech

import java.io.InputStream

/**
 * SentencePiece vocabulary of parakeet-tdt-0.6b-v3: 8192 pieces plus the
 * CTC/transducer blank `<blk>` as the last id.
 *
 * File layout is `"<piece> <id>"` per line; `▁` (U+2581) marks a word
 * boundary and becomes a space, exactly like the onnx-asr reference.
 */
class ParakeetVocabulary private constructor(
    private val pieces: Array<String>,
    val blankId: Int,
) {
    val size: Int get() = pieces.size

    fun decode(ids: List<Int>): String {
        if (ids.isEmpty()) return ""
        val text = StringBuilder()
        for (id in ids) {
            if (id == blankId) continue
            text.append(pieces.getOrNull(id) ?: "")
        }
        // Same cleanup as the reference: single spaces between words, none
        // at the edges.
        return text.toString().trim().replace(Regex("\\s+"), " ")
    }

    companion object {
        fun parse(input: InputStream): ParakeetVocabulary {
            val map = HashMap<Int, String>()
            var blank = -1
            input.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { line ->
                    if (line.isEmpty()) return@forEach
                    val split = line.lastIndexOf(' ')
                    if (split < 0) return@forEach
                    val piece = line.substring(0, split).replace("▁", " ")
                    val id = line.substring(split + 1).toIntOrNull() ?: return@forEach
                    map[id] = piece
                    if (piece == "<blk>") blank = id
                }
            }
            require(map.isNotEmpty()) { "vocab is empty" }
            require(blank >= 0) { "vocab has no <blk> blank" }
            val size = (map.keys.maxOrNull() ?: -1) + 1
            val pieces = Array(size) { map[it] ?: "" }
            return ParakeetVocabulary(pieces, blank)
        }
    }
}
