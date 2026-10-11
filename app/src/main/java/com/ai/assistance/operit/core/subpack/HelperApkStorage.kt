package com.ai.assistance.operit.core.subpack

import android.content.Context
import com.ai.assistance.operit.util.RemoteAsset
import com.ai.assistance.operit.util.RemoteAssetFetcher
import java.io.File

/**
 * Staging for the helper APKs (`shizuku.apk`, `accessibility.apk`).
 *
 * Same mechanics as [SubpackStorage.ensureTemplate], except the target is
 * `cacheDir` rather than a movable leaf: the install flow hands the file to
 * the system installer via FileProvider the moment it is intact, so there is
 * nothing for the user to relocate. Copies extracted by earlier builds keep
 * working — the fetcher recognises them by size+sha and skips the download.
 * A failure is returned, never thrown: the callers own the error surface
 * (the install buttons already toast).
 */
class HelperApkStorage(private val context: Context) {

    suspend fun ensureHelperApk(
        asset: RemoteAsset,
        progress: RemoteAssetFetcher.ProgressListener? = null
    ): Result<File> {
        val dir = context.cacheDir
        return RemoteAssetFetcher(context)
            .ensureAssets(dir, listOf(asset), progress)
            .map { File(it, asset.name) }
    }
}
