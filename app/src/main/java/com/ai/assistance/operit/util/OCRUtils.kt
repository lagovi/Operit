package com.ai.assistance.operit.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.ai.assistance.operit.R
import com.ai.assistance.operit.util.AppLogger
import androidx.annotation.WorkerThread
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** OCR utility: PaddleOCR engine (PP-OCRv6-small det + cyrillic rec). */
object OCRUtils {
    private const val TAG = "OCRUtils"

    // Fork: single PaddleOCR engine replaces ML Kit Latin. The rec dict
    // covers Latin and Cyrillic in one model, so Language is kept only for
    // call-site compatibility and carries no behavior.
    /** Text recognition language option (kept for API compatibility). */
    enum class Language {
        LATIN
    }

    /** Recognition quality option */
    enum class Quality {
        /** Fast, lower precision, for previews or simple text */
        LOW,
        /** Slower, higher precision, upscales small images first */
        HIGH
    }

    /**
     * Preprocess a Bitmap to improve OCR accuracy.
     *
     * @param bitmap source bitmap
     * @return processed bitmap, or the source bitmap when no work is needed
     */
    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        // For high-quality mode we upscale. This significantly improves OCR
        // accuracy on small images. Cap size to avoid OOM.
        val scaleFactor = 2.0f
        val maxDimension = 4096

        val newWidth = (bitmap.width * scaleFactor).toInt()
        val newHeight = (bitmap.height * scaleFactor).toInt()

        if (bitmap.width >= newWidth ||
            bitmap.height >= newHeight ||
            newWidth > maxDimension ||
            newHeight > maxDimension
        ) {
            AppLogger.d(TAG, "Bitmap already large enough, not upscaling for OCR.")
            return bitmap
        }

        AppLogger.d(
            TAG,
            "Upscaling bitmap from ${bitmap.width}x${bitmap.height} to ${newWidth}x${newHeight} for OCR."
        )
        return Bitmap.createScaledBitmap(
            bitmap,
            newWidth,
            newHeight,
            true // filter=true for better quality scaling
        )
    }

    // --------- Low-level API: OCRResult results ---------//

    /**
     * Recognize text from a Bitmap.
     *
     * @param context needed to provision the on-demand engine files
     * @param bitmap image to recognize
     * @param language recognition language (compatibility only)
     * @param quality recognition quality
     * @return recognition result
     */
    @WorkerThread
    suspend fun recognizeTextFromBitmap(
        context: Context,
        bitmap: Bitmap,
        language: Language = Language.LATIN,
        quality: Quality = Quality.LOW
    ): OCRResult {
        val processedBitmap =
            if (quality == Quality.HIGH) {
                preprocessBitmap(bitmap)
            } else {
                bitmap
            }

        return try {
            val lines = PaddleOcrEngine.recognizeLines(context, processedBitmap).getOrElse { e ->
                return OCRResult.Error(e.message ?: "Unknown error")
            }
            OCRResult.Success(fullText = lines.joinToString("\n"), blocks = lines)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error recognizing text from bitmap: ${e.message}", e)
            OCRResult.Error(e.message ?: "Unknown error")
        } finally {
            if (processedBitmap !== bitmap) {
                processedBitmap.recycle()
            }
        }
    }

    /**
     * Recognize text from a Uri.
     *
     * @param context context
     * @param uri image Uri
     * @param language recognition language (compatibility only)
     * @param quality recognition quality
     * @return recognition result
     */
    @WorkerThread
    suspend fun recognizeTextFromUri(
        context: Context,
        uri: Uri,
        language: Language = Language.LATIN,
        quality: Quality = Quality.LOW
    ): OCRResult {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    if (originalBitmap != null) {
                        val result =
                            recognizeTextFromBitmap(context, originalBitmap, language, quality)
                        originalBitmap.recycle()
                        result
                    } else {
                        OCRResult.Error(context.getString(R.string.ocr_cannot_decode_bitmap_from_uri))
                    }
                }
                    ?: OCRResult.Error(context.getString(R.string.ocr_cannot_open_uri_stream))
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error recognizing text from uri: ${e.message}", e)
                OCRResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    // --------- High-level API: simplified, return plain text ---------//

    /**
     * Recognize text in an image.
     *
     * @param context context
     * @param bitmap image
     * @param quality recognition quality
     * @return recognized text, empty string on failure
     */
    @WorkerThread
    suspend fun recognizeText(
        context: Context,
        bitmap: Bitmap,
        quality: Quality = Quality.LOW
    ): String {
        val result = recognizeTextFromBitmap(context, bitmap, Language.LATIN, quality)
        return when (result) {
            is OCRResult.Success -> result.getFullText()
            is OCRResult.Error -> {
                AppLogger.e(TAG, "Text recognition failed: ${result.message}")
                ""
            }
        }
    }

    /**
     * Recognize text in an image (explicit language).
     *
     * @param context context
     * @param bitmap image
     * @param language language (compatibility only)
     * @param quality recognition quality
     * @return recognized text, empty string on failure
     */
    @WorkerThread
    suspend fun recognizeText(
        context: Context,
        bitmap: Bitmap,
        language: Language,
        quality: Quality = Quality.LOW
    ): String {
        val result = recognizeTextFromBitmap(context, bitmap, language, quality)
        return when (result) {
            is OCRResult.Success -> result.getFullText()
            is OCRResult.Error -> {
                AppLogger.e(TAG, "Text recognition failed: ${result.message}")
                ""
            }
        }
    }

    /**
     * Recognize text from a file Uri.
     *
     * @param context context
     * @param uri image Uri
     * @param quality recognition quality
     * @return recognized text, empty string on failure
     */
    @WorkerThread
    suspend fun recognizeText(context: Context, uri: Uri, quality: Quality = Quality.LOW): String {
        val result = recognizeTextFromUri(context, uri, Language.LATIN, quality)
        return when (result) {
            is OCRResult.Success -> result.getFullText()
            is OCRResult.Error -> {
                AppLogger.e(TAG, "Text recognition failed: ${result.message}")
                ""
            }
        }
    }

    /**
     * Scan an image and extract all text lines.
     *
     * @param context needed to provision the on-demand engine files
     * @param bitmap image to recognize
     * @param languages language list by priority (compatibility only)
     * @param quality recognition quality
     * @return all recognized text lines
     */
    @WorkerThread
    suspend fun extractTextBlocks(
        context: Context,
        bitmap: Bitmap,
        languages: List<Language> = listOf(Language.LATIN),
        quality: Quality = Quality.LOW
    ): List<String> {
        if (languages.isEmpty()) return emptyList()
        val result = recognizeTextFromBitmap(context, bitmap, languages.first(), quality)
        return if (result is OCRResult.Success) result.getTextBlocks() else emptyList()
    }

    /**
     * Save a Bitmap to a temp file, for OCR processing.
     *
     * @param context context
     * @param bitmap bitmap
     * @return temp file
     */
    @WorkerThread
    suspend fun saveBitmapToTempFile(context: Context, bitmap: Bitmap): File? =
        withContext(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            val tempFile = File(cacheDir, "ocr_temp_${System.currentTimeMillis()}.jpg")

            try {
                FileOutputStream(tempFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
                return@withContext tempFile
            } catch (e: IOException) {
                AppLogger.e(TAG, "Failed to save bitmap to temp file", e)
                return@withContext null
            }
        }

    /** OCR recognition result */
    sealed class OCRResult {
        /** Recognition success */
        data class Success(val fullText: String, val blocks: List<String>) : OCRResult() {
            /** Full recognized text */
            fun getFullText(): String = fullText

            /** All text lines */
            fun getTextBlocks(): List<String> = blocks

            /** Structured text (one line per row, blank line between blocks) */
            fun getStructuredText(): String = blocks.joinToString("\n")
        }

        /** Recognition failure */
        data class Error(val message: String) : OCRResult()
    }
}
