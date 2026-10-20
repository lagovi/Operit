package com.ai.assistance.operit.data.speech

import com.ai.assistance.operit.util.RemoteAsset

/**
 * The Nemotron 3.5 ASR Streaming 0.6B (int8, 1120 ms chunk) transducer
 * checkpoint, as remote assets.
 *
 * Source: `csukuangfj2/sherpa-onnx-nemotron-3.5-asr-streaming-0.6b-1120ms-int8-2026-06-11`
 * (an official sherpa-onnx export of NVIDIA's Nemotron ASR model, NVIDIA Open
 * Model License). Revision is pinned rather than tracking `main`, so a
 * re-upload cannot change what a user downloads. Encoder/decoder/joiner
 * checksums are the Hugging Face LFS oids; `tokens.txt` is a plain file, so
 * its checksum was computed from the bytes served at this revision.
 *
 * The runtime is separate: the JNI library is not in the APK (it is 24 MB for
 * arm64 alone; see `HANDOFF.md` E28). [RUNTIME_ASSET] is the official
 * sherpa-onnx v1.13.8 static-link AAR; only its arm64
 * `libsherpa-onnx-jni.so` is kept after download, extracted by
 * [NemotronModelStorage.ensureRuntime], so the AAR itself is transient.
 *
 * ~682 MB of weights plus the runtime: an on-demand download, never APK
 * weight. The 1120 ms chunk is the measured best variant (RTF 1.65, quality
 * 6/7 on the test phone; the 560 ms export is slower and no more accurate).
 */
object NemotronModelFiles {

    const val ENCODER_FILE_NAME = "encoder.int8.onnx"
    const val DECODER_FILE_NAME = "decoder.int8.onnx"
    const val JOINER_FILE_NAME = "joiner.int8.onnx"
    const val TOKENS_FILE_NAME = "tokens.txt"

    /** Downloaded AAR; deleted once [RUNTIME_LIB_FILE_NAME] is extracted. */
    const val RUNTIME_AAR_FILE_NAME = "sherpa-onnx-runtime.aar"

    /** Extracted from [RUNTIME_AAR_FILE_NAME]; the `System.load` target. */
    const val RUNTIME_LIB_FILE_NAME = "libsherpa-onnx-jni.so"

    /** AAR member holding the arm64 library; other ABIs are dropped. */
    const val RUNTIME_AAR_ENTRY = "jni/arm64-v8a/libsherpa-onnx-jni.so"

    const val REVISION = "cba1c96ca5ef0e8393b50584ae153a79145dc492"

    private const val BASE_URL =
        "https://huggingface.co/csukuangfj2/sherpa-onnx-nemotron-3.5-asr-streaming-0.6b-1120ms-int8-2026-06-11/resolve/$REVISION"

    private const val SHERPA_RELEASE =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8"

    /** The weights, verified and kept. */
    val ASSETS: List<RemoteAsset> = listOf(
        RemoteAsset(
            name = ENCODER_FILE_NAME,
            url = "$BASE_URL/$ENCODER_FILE_NAME",
            sizeBytes = 657_601_521L,
            sha256 = "2fff2166acaa535bd969fb223c1f0783d71029f143cb298bc54c2afe85abf772"
        ),
        RemoteAsset(
            name = DECODER_FILE_NAME,
            url = "$BASE_URL/$DECODER_FILE_NAME",
            sizeBytes = 14_978_075L,
            sha256 = "19f9c98fc6d0a2c33a65a43b36fdb2e914c26c0aa9764be3aebc502a1e982fb0"
        ),
        RemoteAsset(
            name = JOINER_FILE_NAME,
            url = "$BASE_URL/$JOINER_FILE_NAME",
            sizeBytes = 9_504_438L,
            sha256 = "4101c7c679a0bc30483794b27a059e34e79232aa2068d78d51231a22c8b0d7ce"
        ),
        RemoteAsset(
            name = TOKENS_FILE_NAME,
            url = "$BASE_URL/$TOKENS_FILE_NAME",
            sizeBytes = 131_440L,
            sha256 = "729cc103155bafa785f9cd45746cd41cabe97eab7182fc04d594129587958f8a"
        )
    )

    /**
     * The runtime archive, downloaded and then replaced by the single arm64
     * library inside it. Not part of [ASSETS]: its [RUNTIME_AAR_FILE_NAME] is
     * deleted after extraction, so it must not be required for the model to
     * count as downloaded.
     */
    val RUNTIME_ASSET: RemoteAsset = RemoteAsset(
        name = RUNTIME_AAR_FILE_NAME,
        url = "$SHERPA_RELEASE/sherpa-onnx-static-link-onnxruntime-1.13.8.aar",
        sizeBytes = 38_691_998L,
        sha256 = "b22c3fc1b6a45666d28892bb2f7694beeb77a8362d7ebd77c1a5431ec9435471"
    )

    /** Size of the extracted arm64 library. */
    const val RUNTIME_LIB_BYTES = 24_169_352L

    /** Bytes the weights take on disk, after the AAR is dropped. */
    val WEIGHTS_BYTES: Long = ASSETS.sumOf { it.sizeBytes }

    /** Shown before the user agrees to the download (weights only). */
    val TOTAL_BYTES: Long = WEIGHTS_BYTES
}
