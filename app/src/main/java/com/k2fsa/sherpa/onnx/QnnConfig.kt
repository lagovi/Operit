/*
 * Vendored from sherpa-onnx v1.13.8 `sherpa-onnx/kotlin-api/QnnConfig.kt`
 * (https://github.com/k2-fsa/sherpa-onnx, Apache-2.0).
 */
package com.k2fsa.sherpa.onnx

data class QnnConfig(
    var backendLib: String = "",
    var contextBinary: String = "",
    var systemLib: String = "",
)
