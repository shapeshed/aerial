package com.shapeshed.aerial

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException

/**
 * Read-only provider serving station artwork — rasterized registry logos (SVG -> PNG, see
 * [com.shapeshed.aerial.ui.cachedRemoteArtworkUri]) and locally-cached favourite logos (see
 * [com.shapeshed.aerial.ui.localLogoArtworkUri]) — to external artwork consumers: Android Auto's
 * browse lists, Bluetooth AVRCP, System UI. A stable content:// artworkUri lets those consumers
 * decode once and cache by URI, and — critically for AVRCP — lets Media3 skip decoding a Bitmap
 * for the item at all (embedded artworkData bytes force a decode-and-embed per queue item, which
 * is what overloads the Bluetooth queue-diffing and crashes it; see #123). Exported because media
 * artwork URIs offer no permission-grant handshake; it can only ever open files inside one of its
 * two known cache directories.
 */
class ArtworkProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val context = context ?: throw FileNotFoundException(uri.toString())
        val segments = uri.pathSegments
        val dirName = segments.getOrNull(0) ?: throw FileNotFoundException(uri.toString())
        val fileName = segments.getOrNull(1) ?: throw FileNotFoundException(uri.toString())
        val dir = when (dirName) {
            REGISTRY_ARTWORK_DIR -> File(context.cacheDir, REGISTRY_ARTWORK_DIR)
            LOCAL_LOGO_DIR -> File(context.filesDir, LOCAL_LOGO_DIR)
            else -> throw FileNotFoundException(uri.toString())
        }
        val file = File(dir, fileName)
        if (file.canonicalFile.parentFile != dir.canonicalFile || !file.exists()) {
            throw FileNotFoundException(uri.toString())
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String {
        val extension = uri.lastPathSegment?.substringAfterLast('.', "").orEmpty()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/*"
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        const val REGISTRY_ARTWORK_DIR = "registry_artwork"

        // Matches the download directory MainViewModel.downloadLogo caches favourited logos in.
        const val LOCAL_LOGO_DIR = "logos"

        fun uriFor(context: android.content.Context, dir: String, fileName: String): Uri =
            Uri.parse("content://${context.packageName}.artwork/$dir/$fileName")
    }
}
