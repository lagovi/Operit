package com.ai.assistance.operit.api.speech

import kotlinx.serialization.Serializable

/**
 * The acoustic contract of one offline CTC checkpoint: how raw audio becomes
 * the mel frames the graph eats, and how its outputs map back to text.
 *
 * Every field has a default spelling the GigaAM-Multilingual int8 export,
 * because that checkpoint is the reference the oracle tests pin. A custom
 * model from a Hugging Face link fills in what differs; anything it leaves
 * at the default is asserted against the same reference path, so a custom
 * model with GigaAM's numbers behaves bit-identically to the built-in one.
 *
 * Deliberately acoustic-only: file names, URLs and checksums live in the
 * model registry entry, not here.
 */
@Serializable
data class CtcModelConfig(
    val sampleRate: Int = 16000,
    val nMels: Int = 64,
    val winLength: Int = 320,
    val hopLength: Int = 160,
    val nFft: Int = 320,
    val center: Boolean = false,
    val subsamplingFactor: Int = 4,
    /**
     * CTC blank class id, or -1 for "last id", which is where sherpa-onnx CTC
     * exports put `<blk>`. A non-negative value must be a valid id.
     */
    val blankId: Int = -1,
) {
    init {
        require(sampleRate > 0) { "sampleRate must be positive: $sampleRate" }
        require(nMels > 0) { "nMels must be positive: $nMels" }
        require(winLength > 0) { "winLength must be positive: $winLength" }
        require(hopLength > 0) { "hopLength must be positive: $hopLength" }
        require(nFft >= winLength) { "nFft $nFft must cover winLength $winLength" }
        require(!center) { "center=true needs reflect padding, which is not implemented" }
        require(subsamplingFactor > 0) { "subsamplingFactor must be positive: $subsamplingFactor" }
        require(blankId >= -1) { "blankId must be -1 or a class id: $blankId" }
    }

    companion object {
        val GIGAAM_DEFAULT = CtcModelConfig()
    }
}
