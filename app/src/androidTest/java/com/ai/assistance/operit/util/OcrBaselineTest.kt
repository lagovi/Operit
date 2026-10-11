package com.ai.assistance.operit.util

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * OCR baseline (manual-run only, NOT part of CI gates): measures the
 * bundled ML Kit Latin engine on fixed test images.
 *
 * Setup (once): adb push <5 png> /sdcard/ocr_baseline/
 * Run: adb shell am instrument -w -e class
 *   com.ai.assistance.operit.util.OcrBaselineTest
 *   com.ai.assistance.operit.debug.test/androidx.test.runner.AndroidJUnitRunner
 * Read: adb shell cat /sdcard/ocr_baseline/results.txt
 *
 * Each image is recognized with Quality LOW and HIGH; per run the test
 * records milliseconds + full text. Accuracy is scored OFF-device against
 * the ground-truth .txt files (kept next to the pngs on the host).
 */
@RunWith(AndroidJUnit4::class)
class OcrBaselineTest {

    @Test fun ocrBaseline_measure() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File("/sdcard/ocr_baseline")
        val out = File(dir, "results.txt")
        val sb = StringBuilder()
        // Warm-up: first ML Kit call includes model init, not representative.
        val warmups =
            dir.listFiles { f -> f.extension == "png" }?.sortedBy { it.name } ?: emptyList()
        check(warmups.isNotEmpty()) { "no pngs in /sdcard/ocr_baseline" }
        runBlocking {
            val warmBmp = BitmapFactory.decodeFile(warmups.first().absolutePath)
            OCRUtils.recognizeText(ctx, warmBmp, OCRUtils.Quality.LOW)
            sb.appendLine("warmup done on ${warmups.first().name}")
            for (img in warmups) {
                val bmp = BitmapFactory.decodeFile(img.absolutePath)
                check(bmp != null) { "cannot decode ${img.name}" }
                for (q in listOf(OCRUtils.Quality.LOW, OCRUtils.Quality.HIGH)) {
                    val t0 = System.nanoTime()
                    val text = OCRUtils.recognizeText(ctx, bmp, q)
                    val ms = (System.nanoTime() - t0) / 1_000_000
                    sb.appendLine("== ${img.name} [$q] ${ms}ms len=${text.length}")
                    sb.appendLine(text.trim())
                    sb.appendLine("--")
                }
            }
        }
        out.writeText(sb.toString())
    }
}
