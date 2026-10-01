package com.ai.assistance.operit.data.speech

import com.ai.assistance.operit.util.RemoteAsset

/**
 * The GigaAM-Multilingual int8 checkpoint, as remote assets.
 *
 * Two repositories publish this export. Comparing all 959 ONNX initializers
 * shows they are bit-identical; the 10557 differing bytes are protobuf
 * serialisation plus the `language` metadata string. i2z1 is used because it
 * ships `export-multilingual-ctc.py`, which documents the method as
 * `quantize_dynamic` with QUInt8 weights. The other repository it was compared
 * against, DmitrySharonov/GigaAM-Multilingual-ONNX, is FP32 only and its
 * `large_ctc` file is truncated to 3.4 MB.
 *
 * Revision is pinned rather than tracking `main`, so a re-upload cannot change
 * what a user downloads. `tokens.txt` is a plain file, so it has no LFS oid and
 * the checksum here was computed from the bytes served at this revision.
 *
 * The vocabulary has no digits, no uppercase and no punctuation beyond the
 * apostrophe. That is the checkpoint, not this manifest.
 */
object GigaAMModelFiles {

    const val MODEL_FILE_NAME = "model.int8.onnx"
    const val TOKENS_FILE_NAME = "tokens.txt"
    const val REVISION = "ba9011bfb2e52cacad5a12e5d9346392d7825c76"

    private const val BASE_URL =
        "https://huggingface.co/i2z1/gigaam-multilingual-ctc-onnx-int8/resolve/$REVISION"

    val ASSETS: List<RemoteAsset> = listOf(
        RemoteAsset(
            name = MODEL_FILE_NAME,
            url = "$BASE_URL/$MODEL_FILE_NAME",
            sizeBytes = 224_762_512L,
            sha256 = "5f584553e20db1af7e0fff98728b6904b670eb6488b88da8e87cda7870a6235f"
        ),
        RemoteAsset(
            name = TOKENS_FILE_NAME,
            url = "$BASE_URL/$TOKENS_FILE_NAME",
            sizeBytes = 391L,
            sha256 = "9b5df7987cb4ca52c1a468649ce897fab1cd182067416e29fef49dfaa7a856c2"
        )
    )

    /** Shown before the user agrees to the download. */
    val TOTAL_BYTES: Long = ASSETS.sumOf { it.sizeBytes }
}