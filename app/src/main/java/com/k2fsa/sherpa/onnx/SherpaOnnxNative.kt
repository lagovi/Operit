/*
 * Loader for the sherpa-onnx JNI library used by the Nemotron engine.
 *
 * The upstream Kotlin API (vendored next to this file, package
 * `com.k2fsa.sherpa.onnx`) normally calls `System.loadLibrary`, which only
 * finds libraries packaged in the APK. This fork instead ships the 24 MB
 * arm64 `libsherpa-onnx-jni.so` as part of the on-demand Nemotron payload
 * (`OfflinePayloads.NEMOTRON`), the same shape OpenCV already uses in
 * `OfflinePayloads.PADDLE_OCR`, so the APK stays lean.
 *
 * The first recognizer must call [ensureLoaded] with the extracted library
 * path before any `com.k2fsa.sherpa.onnx` class is instantiated. The vendored
 * companions were edited to drop their `loadLibrary` init: loading through
 * the package manager would fail here, and a second `dlopen` of the same
 * library is what we must avoid.
 */
package com.k2fsa.sherpa.onnx

import java.io.File

object SherpaOnnxNative {
    @Volatile
    private var loaded = false

    /** Loads [library] once; further calls are no-ops. Throws if unreadable. */
    @Synchronized
    fun ensureLoaded(library: File) {
        if (loaded) return
        if (!library.isFile) {
            throw IllegalStateException("sherpa-onnx runtime is missing: ${library.absolutePath}")
        }
        System.load(library.absolutePath)
        loaded = true
    }
}
