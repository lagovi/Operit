package com.ai.assistance.operit.data.speech

import com.ai.assistance.operit.util.RemoteAsset

/**
 * The parakeet-tdt-0.6b-v3 int8 checkpoint, as remote assets.
 *
 * Source: `istupakov/parakeet-tdt-0.6b-v3-onnx` (NeMo export +
 * `quantize_dynamic`; encoder QUInt8, joint QInt8). The sherpa-layout
 * `encoder.int8.onnx` is the same weights under another name, so the
 * onnx-asr layout is used: the fused `decoder_joint` needs one ORT
 * session instead of two.
 *
 * Revision is pinned rather than tracking `main`, so a re-upload cannot
 * change what a user downloads. The `vocab.txt` checksum was computed
 * from the bytes served at this revision (plain file, no LFS oid).
 *
 * ~670 MB total: this is an on-demand download, never APK weight. It fits
 * the test phone's SD card (980 MB free) with ~300 MB headroom.
 */
object ParakeetModelFiles {

    const val ENCODER_FILE_NAME = "encoder-model.int8.onnx"
    const val JOINT_FILE_NAME = "decoder_joint-model.int8.onnx"
    const val VOCAB_FILE_NAME = "vocab.txt"
    const val REVISION = "8f23f0c03c8761650bdb5b40aaf3e40d2c15f1ce"

    private const val BASE_URL =
        "https://huggingface.co/istupakov/parakeet-tdt-0.6b-v3-onnx/resolve/$REVISION"

    val ASSETS: List<RemoteAsset> = listOf(
        RemoteAsset(
            name = ENCODER_FILE_NAME,
            url = "$BASE_URL/$ENCODER_FILE_NAME",
            sizeBytes = 652_183_999L,
            sha256 = "6139d2fa7e1b086097b277c7149725edbab89cc7c7ae64b23c741be4055aff09"
        ),
        RemoteAsset(
            name = JOINT_FILE_NAME,
            url = "$BASE_URL/$JOINT_FILE_NAME",
            sizeBytes = 18_202_004L,
            sha256 = "eea7483ee3d1a30375daedc8ed83e3960c91b098812127a0d99d1c8977667a70"
        ),
        RemoteAsset(
            name = VOCAB_FILE_NAME,
            url = "$BASE_URL/$VOCAB_FILE_NAME",
            sizeBytes = 93_939L,
            sha256 = "d58544679ea4bc6ac563d1f545eb7d474bd6cfa467f0a6e2c1dc1c7d37e3c35d"
        )
    )

    /** Shown before the user agrees to the download. */
    val TOTAL_BYTES: Long = ASSETS.sumOf { it.sizeBytes }
}
