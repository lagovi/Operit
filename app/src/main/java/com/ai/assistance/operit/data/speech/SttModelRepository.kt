package com.ai.assistance.operit.data.speech

import android.content.Context
import com.ai.assistance.operit.R
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.util.OperitPaths
import com.ai.assistance.operit.util.RemoteAsset
import com.ai.assistance.operit.util.RemoteAssetFetcher
import java.io.File

/**
 * The local speech-to-text model, fetched on first use.
 *
 * Fork: these files were bundled in the APK. The encoder alone is 121 MiB, and
 * local STT is a feature a minority of installs open, so every user was paying
 * for it at install time. The model itself is unchanged — same upstream build,
 * same checksums — only the delivery moved from the APK to the first launch of
 * the feature that needs it.
 */
class SttModelRepository(private val context: Context) {

    private val fetcher = RemoteAssetFetcher(context)

    /**
     * Ensure the sherpa-ncnn model is present on disk.
     *
     * @return the directory holding the verified model files
     */
    suspend fun ensureNcnnModel(
            onProgress: RemoteAssetFetcher.ProgressListener? = null
    ): Result<File> {
        val target = File(OperitPaths.sherpaNcnnModelsDir(context), MODEL_DIR_NAME)
        val result = fetcher.ensureAssets(target, ncnnAssets(), onProgress)
        if (result.isFailure) {
            AppLogger.e(
                TAG,
                "Local STT model is unavailable: ${result.exceptionOrNull()?.message}"
            )
        }
        return result
    }

    private fun ncnnAssets(): List<RemoteAsset> =
            listOf(
                    RemoteAsset(
                            name = "encoder_jit_trace-pnnx.ncnn.param",
                            url =
                                    "$BASE_URL/encoder_jit_trace-pnnx.ncnn.param",
                            sizeBytes = 161_888,
                            sha256 =
                                    "97ad0954fb2cb4730f87a7eb66401b024f756752ece246e4b2063f870ebf3e18"
                    ),
                    RemoteAsset(
                            name = "encoder_jit_trace-pnnx.ncnn.bin",
                            url = "$BASE_URL/encoder_jit_trace-pnnx.ncnn.bin",
                            sizeBytes = 127_364_056,
                            sha256 =
                                    "4ed65f05b78c0106d3d176018ab01e26a15c200604490d3d49b08cc75a122dd0"
                    ),
                    RemoteAsset(
                            name = "decoder_jit_trace-pnnx.ncnn.param",
                            url = "$BASE_URL/decoder_jit_trace-pnnx.ncnn.param",
                            sizeBytes = 439,
                            sha256 =
                                    "cb88f5894978fd3e85369d2f8ea55621809fceb2b5158243fb0cd025eb4f1aaf"
                    ),
                    RemoteAsset(
                            name = "decoder_jit_trace-pnnx.ncnn.bin",
                            url = "$BASE_URL/decoder_jit_trace-pnnx.ncnn.bin",
                            sizeBytes = 6_412_296,
                            sha256 =
                                    "dc4df2d8e1ddee1b90ac72a2de982eb1d320ee6c9a70e1dee4d23d9acfc8b978"
                    ),
                    RemoteAsset(
                            name = "joiner_jit_trace-pnnx.ncnn.param",
                            url = "$BASE_URL/joiner_jit_trace-pnnx.ncnn.param",
                            sizeBytes = 490,
                            sha256 =
                                    "46c339f3869136c2f6d9d9d6983a6cbc2bfbcd0e3dab0f76ae25e9477f00a360"
                    ),
                    RemoteAsset(
                            name = "joiner_jit_trace-pnnx.ncnn.bin",
                            url = "$BASE_URL/joiner_jit_trace-pnnx.ncnn.bin",
                            sizeBytes = 7_350_724,
                            sha256 =
                                    "0e6c4370017394de5d74128756233d2e4451209e63ac2abd525da3b089e8bee1"
                    ),
                    RemoteAsset(
                            name = "tokens.txt",
                            url = "$BASE_URL/tokens.txt",
                            sizeBytes = 56_317,
                            sha256 =
                                    "a8e0e4ec53810e433789b54a5c0134a7eaa2ffca595a6334d54c00da858841d3"
                    )
            )

    /** Human-readable reason a local STT attempt cannot proceed. */
    fun unavailableMessage(): String = context.getString(R.string.stt_model_unavailable)

    companion object {
        private const val TAG = "SttModelRepository"

        const val MODEL_DIR_NAME = "sherpa-ncnn-streaming-zipformer-bilingual-zh-en-2023-02-13"

        // csukuangfj/sherpa-ncnn-streaming-zipformer-bilingual-zh-en-2023-02-13
        // pinned at commit 05945efc40afe4b572542f01104ca5c413a9f6e1. The same
        // revision is recorded in the removed app/config/stt-model-assets.properties.
        private const val BASE_URL =
                "https://huggingface.co/csukuangfj/sherpa-ncnn-streaming-zipformer-bilingual-zh-en-2023-02-13/resolve/05945efc40afe4b572542f01104ca5c413a9f6e1"
    }
}
