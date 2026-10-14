package com.ai.assistance.operit.util

import android.content.Context
import com.paddle.ocr.EngineConfig
import com.paddle.ocr.PaddleOCR
import com.paddle.ocr.PaddleOCRConfig
import com.paddle.ocr.util.OpenCVUtils
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * PaddleOCR engine holder (PP-OCRv6-small det + cyrillic PP-OCRv5-mobile
 * rec: one rec model covers Latin and Russian).
 *
 * The SDK sources under `com.paddle.ocr` are upstream PaddleOCR
 * (Apache-2.0) with a 3-site fork: models and the OpenCV .so load from
 * filesDir instead of assets/APK (SIZE goals), provisioned on first use
 * from the fork's own release via [OfflinePayload.PADDLE_OCR].
 * There is no second engine and no silent degradation: if provisioning
 * fails the caller gets the failure to surface.
 */
object PaddleOcrEngine {
    private const val TAG = "PaddleOcrEngine"
    private const val LEAF = "ocr"

    private val initMutex = Mutex()
    private var instance: PaddleOCR? = null

    /**
     * Recognize bitmap lines in reading order, joined with newlines
     * (same shape as the old ML Kit full text).
     */
    suspend fun recognizeLines(appContext: Context, bitmap: android.graphics.Bitmap): Result<List<String>> {
        val engine = try {
            ensureEngine(appContext)
        } catch (e: Exception) {
            AppLogger.e(TAG, "OCR engine unavailable", e)
            return Result.failure(e)
        }
        return try {
            val run = engine.recognize(bitmap)
            AppLogger.d(
                TAG,
                "OCR done: ${run.lineCount} lines, " +
                    "det=${run.detectionTimeMs}ms rec=${run.recognitionTimeMs}ms " +
                    "total=${run.totalTimeMs}ms"
            )
            Result.success(run.results.map { it.text })
        } catch (e: Exception) {
            AppLogger.e(TAG, "OCR inference failed", e)
            Result.failure(e)
        }
    }

    private suspend fun ensureEngine(appContext: Context): PaddleOCR {
        synchronized(this) {
            instance?.let { return it }
        }
        return initMutex.withLock {
            instance?.let { return it }
            val ctx = appContext.applicationContext
            val dir = File(ctx.filesDir, LEAF)
            val fetch = RemoteAssetFetcher(ctx)
                .ensureAssets(dir, OfflinePayload.PADDLE_OCR.assets)
                .getOrElse { throw it }
            val soFile = File(fetch, "libopencv_java4.so")
            // Intent: the OpenCV build needs a newer libc++ than the APK
            // ships (missing __sfp_handle_exceptions), and the process may
            // already hold the old one under the same soname. So both travel
            // on-demand with an isolated soname (libc++_paddle.so, see the
            // payload notes) and load in dependency order. If skipped:
            // dlopen binds the stale process libc++ and fails.
            val cxxFile = File(fetch, "libc++_paddle.so")
            if (!OpenCVUtils.initWithSo(cxxFile, soFile)) {
                throw IllegalStateException("Could not load OpenCV from ${soFile.absolutePath}")
            }
            val created = PaddleOCR.create(
                context = ctx,
                config = PaddleOCRConfig(),
                engineConfig = EngineConfig(),
                detModelAssetPath = File(fetch, "det.onnx").absolutePath,
                recModelAssetPath = File(fetch, "rec.onnx").absolutePath,
                recConfigAssetPath = File(fetch, "rec.yml").absolutePath,
            )
            AppLogger.d(TAG, "OCR engine ready, cold load ${created.coldLoadTimeMs}ms")
            synchronized(this) {
                instance = created
            }
            created
        }
    }
}
