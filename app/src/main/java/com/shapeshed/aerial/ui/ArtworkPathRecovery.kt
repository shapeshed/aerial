package com.shapeshed.aerial.ui

import java.io.File

/** Keeps a cached logo when present, otherwise falls back to the registry URL. */
internal fun recoverLogoPath(storedPath: String, remoteLogoUrl: String, fileExists: Boolean): String =
    when {
        fileExists -> storedPath
        remoteLogoUrl.isNotBlank() -> remoteLogoUrl
        else -> storedPath
    }

/** Chooses artwork for a recently-played row when its saved file may be stale. */
internal fun recentlyPlayedLogoPath(storedPath: String, remoteLogoUrl: String): String =
    recoverLogoPath(
        storedPath = storedPath,
        remoteLogoUrl = remoteLogoUrl,
        fileExists = storedPath.isNotBlank() && !storedPath.startsWith("http") && File(storedPath).isFile,
    )

/** Removes a station's cached artwork and its Media3-derived companion image. */
internal fun deleteStationArtworkFiles(logoPath: String) {
    if (logoPath.isBlank() || logoPath.startsWith("http")) return
    val file = File(logoPath)
    file.delete()
    File(file.parentFile, "${file.nameWithoutExtension}_media.png").delete()
}
