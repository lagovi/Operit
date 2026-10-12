package com.ai.assistance.operit.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith

/**
 * OCR baseline (manual-run only, NOT part of CI gates): measures the
 * bundled ML Kit Latin engine on fixed test images.
 *
 * No coroutines here on purpose: the minified test APK must not depend
 * on kotlinx-coroutines runtime pieces R8 strips (crashed 11.10).
 * ML Kit's Task API + Tasks.await is enough, and mirrors what
 * OCRUtils does (LOW = direct process, HIGH = 2x upscale first).
 *
 * Setup (once): adb push <5 png> /sdcard/ocr_baseline/
 * Run: adb shell am instrument -w -e class
 *   com.ai.assistance.operit.util.OcrBaselineTest
 *   com.ai.assistance.operit.debug.test/androidx.test.runner.AndroidJUnitRunner
 * Read: adb shell cat /sdcard/ocr_baseline/results.txt
 *
 * Accuracy is scored OFF-device against the ground-truth .txt files
 * (kept next to the pngs on the host).
 */
@RunWith(AndroidJUnit4::class)
class OcrBaselineTest {

    private fun upscale2x(bitmap: Bitmap): Bitmap {
        val w = (bitmap.width * 2.0f).toInt()
        val h = (bitmap.height * 2.0f).toInt()
        if (w > 4096 || h > 4096) return bitmap
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    @Test fun ocrBaseline_measure() {
        val dir = File("/sdcard/ocr_baseline")
        val imgs = dir.listFiles { f -> f.extension == "png" }?.sortedBy { it.name } ?: emptyList()
        check(imgs.isNotEmpty()) { "no pngs in /sdcard/ocr_baseline" }
        val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val sb = StringBuilder()
        // Warm-up: first ML Kit call includes model init, not representative.
        Tasks.await(client.process(InputImage.fromFilePathOrNull(imgs.first())), 120, TimeUnit.SECONDS)
        sb.appendLine("warmup done on ${imgs.first().name}")
        for (img in imgs) {
            val bmp = BitmapFactory.decodeFile(img.absolutePath)
            check(bmp != null) { "cannot decode ${img.name}" }
            for ((q, b) in listOf("LOW" to bmp, "HIGH" to upscale2x(bmp))) {
                val t0 = System.nanoTime()
                val text = Tasks.await(client.process(InputImage.fromBitmap(b, 0)), 120, TimeUnit.SECONDS).text
                val ms = (System.nanoTime() - t0) / 1_000_000
                sb.appendLine("== ${img.name} [$q] ${ms}ms len=${text.length}")
                sb.appendLine(text.trim())
                sb.appendLine("--")
            }
        }
        client.close()
        File(dir, "results.txt").writeText(sb.toString())
    }

    private fun InputImage.Companion.fromFilePathOrNull(file: File): InputImage {
        return InputImage.fromFilePath(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
            android.net.Uri.fromFile(file)
        )
    }
}
