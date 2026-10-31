package com.ai.assistance.operit.data.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.ai.assistance.operit.util.AppLogger
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Stores the square image avatars used by character cards, group cards and
 * user portraits. Every creator (preset, device import, draw, generate) ends
 * up here, so the stored contract stays a `file://` URI in the existing keys.
 */
object AvatarAssetStore {

    private const val TAG = "AvatarAssetStore"
    private const val DIR_NAME = "avatar_assets"
    private const val SIDE_PX = 512

    fun directory(context: Context): File = File(context.filesDir, DIR_NAME).apply { mkdirs() }

    suspend fun saveBitmap(
        context: Context,
        bitmap: Bitmap,
        prefix: String
    ): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val square = centerCropSquare(bitmap, SIDE_PX)
            val file = File(directory(context), "${prefix}_${UUID.randomUUID()}.png")
            file.outputStream().use { output ->
                square.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            if (square !== bitmap) square.recycle()
            Uri.fromFile(file)
        }.onFailure {
            AppLogger.e(TAG, "Failed to save avatar bitmap for $prefix", it)
        }.getOrNull()
    }

    suspend fun saveImageBitmap(
        context: Context,
        image: ImageBitmap,
        prefix: String
    ): Uri? = withContext(Dispatchers.IO) {
        val bitmap = Bitmap.createBitmap(
            image.width,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawBitmap(image.asAndroidBitmap(), 0f, 0f, null)
        saveBitmap(context, bitmap, prefix)
    }

    suspend fun saveBytes(
        context: Context,
        bytes: ByteArray,
        prefix: String
    ): Uri? = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: run {
                AppLogger.w(TAG, "Could not decode avatar bytes for $prefix")
                return@withContext null
            }
        saveBitmap(context, bitmap, prefix)
    }

    suspend fun saveBase64(
        context: Context,
        base64: String,
        prefix: String
    ): Uri? = withContext(Dispatchers.IO) {
        val cleaned = base64.substringAfter(',', base64)
        val bytes = runCatching { android.util.Base64.decode(cleaned, android.util.Base64.DEFAULT) }
            .onFailure { AppLogger.e(TAG, "Bad base64 image for $prefix", it) }
            .getOrNull()
            ?: return@withContext null
        saveBytes(context, bytes, prefix)
    }

    fun encodePng(bitmap: Bitmap): ByteArray {
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
    }

    private fun centerCropSquare(source: Bitmap, size: Int): Bitmap {
        val side = minOf(source.width, source.height)
        val left = (source.width - side) / 2
        val top = (source.height - side) / 2
        val cropped = Bitmap.createBitmap(source, left, top, side, side)
        val scaled = Bitmap.createScaledBitmap(cropped, size, size, true)
        if (cropped !== scaled && cropped !== source) cropped.recycle()
        return scaled
    }
}
