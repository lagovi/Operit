package com.ai.assistance.operit.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.StatFs
import java.io.File

/**
 * How much space the installed app occupies, split the way the user asked:
 * internal flash, the memory card, and the total.
 *
 * Internal is what the system cannot move: the APK file itself plus the
 * private data dir. The `oat` directory next to the APK (compiled dex) is
 * not readable by the app, so internal is a slight undercount and is
 * labelled as app + data, not as everything.
 *
 * The card counts only this app's own directory on the removable volume;
 * the rest of the card is not this app's business.
 */
data class AppStorageBreakdown(
    val apkBytes: Long,
    val appDataBytes: Long,
    val sdCardBytes: Long,
    val internalFreeBytes: Long,
    val sdCardFreeBytes: Long,
    val sdCardTotalBytes: Long,
) {
    val internalBytes: Long get() = apkBytes + appDataBytes
    val totalBytes: Long get() = internalBytes + sdCardBytes
    val hasSdCard: Boolean get() = sdCardTotalBytes > 0
}

object AppStorageUsage {

    /** Reads sizes off disk; call on Dispatchers.IO, never on the UI thread. */
    fun measure(context: Context): AppStorageBreakdown {
        val apkBytes = try {
            val info = context.packageManager.getApplicationInfo(context.packageName, 0)
            var bytes = File(info.sourceDir).length()
            info.splitSourceDirs?.forEach { bytes += File(it).length() }
            bytes
        } catch (e: PackageManager.NameNotFoundException) {
            AppLogger.e(TAG, "Cannot find our own package", e)
            0L
        }
        val dataDir = File(context.applicationInfo.dataDir)
        val appDataBytes = dirSize(dataDir)
        val internalFreeBytes = freeBytesOf(dataDir)

        var sdCardBytes = 0L
        var sdCardFreeBytes = -1L
        var sdCardTotalBytes = 0L
        val sdRoot = AppDataLocation(context).dirFor(AppDataLocation.Location.SDCARD_APP, "")
        if (sdRoot != null) {
            sdCardBytes = dirSize(sdRoot)
            try {
                val stat = StatFs(sdRoot.path)
                sdCardFreeBytes = stat.availableBytes
                sdCardTotalBytes = stat.totalBytes
            } catch (e: Exception) {
                AppLogger.e(TAG, "Cannot stat the memory card", e)
            }
        }
        return AppStorageBreakdown(
            apkBytes = apkBytes,
            appDataBytes = appDataBytes,
            sdCardBytes = sdCardBytes,
            internalFreeBytes = freeBytesOf(dataDir),
            sdCardFreeBytes = sdCardFreeBytes,
            sdCardTotalBytes = sdCardTotalBytes,
        )
    }

    /** Recursive size; pure so the rule is unit-testable. */
    internal fun dirSize(dir: File?): Long {
        if (dir == null || !dir.isDirectory) return 0L
        var total = 0L
        val children = dir.listFiles() ?: return 0L
        for (child in children) {
            total += if (child.isDirectory) dirSize(child) else child.length()
        }
        return total
    }

    private fun freeBytesOf(dir: File): Long {
        return try {
            StatFs(dir.path).availableBytes
        } catch (e: Exception) {
            AppLogger.e(TAG, "Cannot stat ${dir.path}", e)
            -1L
        }
    }

    private const val TAG = "AppStorageUsage"
}
