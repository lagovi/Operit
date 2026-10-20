/*
 * Vendored from sherpa-onnx v1.13.8
 * `sherpa-onnx/kotlin-api/HomophoneReplacerConfig.kt`
 * (https://github.com/k2-fsa/sherpa-onnx, Apache-2.0).
 */
package com.k2fsa.sherpa.onnx

data class HomophoneReplacerConfig(
    var dictDir: String = "", // unused
    var lexicon: String = "",
    var ruleFsts: String = "",
)
