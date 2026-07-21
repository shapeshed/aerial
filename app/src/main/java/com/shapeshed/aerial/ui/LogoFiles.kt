package com.shapeshed.aerial.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import com.shapeshed.aerial.ArtworkProvider
import com.shapeshed.aerial.R
import android.webkit.MimeTypeMap
import androidx.core.graphics.createBitmap
import coil3.BitmapImage
import coil3.Image
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.svg.SvgDecoder
import java.io.File
import java.net.URL
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.withTimeoutOrNull

private const val ARTWORK_FETCH_TIMEOUT_MS = 3_000L

suspend fun copyLogoFromUri(context: Context, uri: Uri, directory: File): File? {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri)
    val extension = extensionFromMimeType(mimeType)
        ?: uri.lastPathSegment?.extensionOrNull()
        ?: "img"
    val source = contentResolver.openInputStream(uri) ?: return null
    return source.use { input ->
        val dest = File(directory, "${UUID.randomUUID()}.$extension")
        dest.outputStream().use { output -> input.copyTo(output) }
        ensureMediaArtworkForLogo(context, dest)
        dest
    }
}

fun logoFileForUrl(url: String, directory: File, contentType: String?): File {
    val mimeType = contentType?.substringBefore(';')?.trim()
    val extension = extensionFromMimeType(mimeType)
        ?: URL(url).path.extensionOrNull()
        ?: "img"

    return File(directory, "${UUID.randomUUID()}.$extension")
}

@Volatile private var svgLoader: ImageLoader? = null

private fun svgLoader(context: Context): ImageLoader =
    svgLoader ?: ImageLoader.Builder(context.applicationContext)
        .components { add(SvgDecoder.Factory()) }
        .build()
        .also { svgLoader = it }

suspend fun ensureMediaArtworkForLogo(context: Context, file: File): File {
    if (file.extension.lowercase(Locale.US) != "svg") return file

    val pngFile = mediaArtworkFile(file)
    if (pngFile.exists()) return pngFile

    return try {
        val request = ImageRequest.Builder(context)
            .data(file)
            .size(512)
            .build()
        val result = svgLoader(context).execute(request) as? SuccessResult ?: return file
        val bitmap = result.image.toOpaqueBitmap()
        pngFile.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        pngFile
    } catch (_: Exception) {
        file
    }
}

/**
 * Renders a remote logo Android Auto can't fetch or decode itself to a PNG cached on disk,
 * keyed by URL, and returns a stable content:// URI served by
 * [com.shapeshed.aerial.ArtworkProvider]. Returns null for logos Auto handles fine directly.
 *
 * Two classes need this proxying. SVGs: surfaces that render a MediaItem's artworkUri
 * themselves — Auto's browse lists and mini player — can't decode SVG (only the actively
 * playing session's artwork goes through the app's SVG-capable
 * [com.shapeshed.aerial.CoilBitmapLoader]). Cleartext http URLs: Auto fetches artworkUri in
 * its own process, which blocks cleartext, while this app permits it (see
 * network_security_config.xml — many station streams and logos are http-only). Handing Auto a
 * content URI keeps its normal decode-once-and-cache-by-URI behaviour, which embedded
 * artworkData bytes would defeat (visible as icons flashing in on every list render).
 */
suspend fun cachedRemoteArtworkUri(context: Context, logoUrl: String): Uri? {
    if (!logoUrl.startsWith("http")) return null
    val isSvg = logoUrl.substringBefore('?').lowercase(Locale.US).endsWith(".svg")
    val isCleartext = logoUrl.startsWith("http://")
    if (!isSvg && !isCleartext) return null

    val cacheDir = File(context.cacheDir, ArtworkProvider.ARTWORK_CACHE_DIR)
    val pngFile = File(cacheDir, "${logoUrl.hashCode().toUInt()}.png")
    if (pngFile.exists()) return ArtworkProvider.uriFor(context, pngFile.name)

    return try {
        val request = ImageRequest.Builder(context)
            .data(logoUrl)
            .size(512)
            .build()
        // The singleton loader (AerialApp) has both the SvgDecoder and the User-Agent-sending
        // HTTP client some logo hosts require; the local svgLoader is file-only. The timeout
        // bounds how long a browse list can stall on one slow host — on miss the icon just
        // falls back until a later request re-tries.
        val result = withTimeoutOrNull(ARTWORK_FETCH_TIMEOUT_MS) {
            SingletonImageLoader.get(context).execute(request)
        } as? SuccessResult ?: return null
        val bitmap = result.image.toOpaqueBitmap()
        cacheDir.mkdirs()
        // Write-then-rename so a concurrent request (Android Auto prefetches folders in
        // parallel) or a mid-write process kill can never expose a truncated PNG under the
        // final name — exists() above only ever sees complete files.
        val tmpFile = File(cacheDir, "${pngFile.name}.${UUID.randomUUID()}.tmp")
        tmpFile.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        if (!tmpFile.renameTo(pngFile)) {
            tmpFile.delete()
            if (!pngFile.exists()) return null
        }
        ArtworkProvider.uriFor(context, pngFile.name)
    } catch (_: Exception) {
        null
    }
}

fun appIconBitmap(context: Context): ByteArray? {
    return try {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.aerial_icon_artwork)
            ?: return null
        val output = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        output.toByteArray()
    } catch (_: Exception) {
        null
    }
}

fun compressedLogoData(file: File): ByteArray? {
    val artworkFile = if (file.extension.lowercase(Locale.US) == "svg") {
        mediaArtworkFile(file).takeIf { it.exists() } ?: return null
    } else {
        file
    }
    if (!artworkFile.exists()) return null
    return when (artworkFile.extension.lowercase(Locale.US)) {
        "png", "jpg", "jpeg", "webp" -> artworkFile.readBytes()
        else -> {
            // Unknown extension (e.g. .img) — try BitmapFactory and re-encode as JPEG
            val bitmap = BitmapFactory.decodeFile(artworkFile.absolutePath) ?: return null
            val out = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }
}

private fun mediaArtworkFile(file: File): File {
    return if (file.extension.lowercase(Locale.US) == "svg") {
        File(file.parentFile, "${file.nameWithoutExtension}_media.png")
    } else {
        file
    }
}

private fun extensionFromMimeType(mimeType: String?): String? {
    return mimeType?.let {
        MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(it)
            ?.lowercase(Locale.US)
    }
}

private fun String.extensionOrNull(): String? {
    return substringAfterLast('.', missingDelimiterValue = "")
        .lowercase(Locale.US)
        .takeIf { it.isNotBlank() && it.length <= 5 }
}

/**
 * Rasterizes a Coil [Image] onto an opaque background. System media surfaces (quick controls,
 * lock screen, Bluetooth AVRCP, Android Auto) draw an artwork bitmap with no guaranteed
 * background behind it, so a source image with transparent areas can end up invisible against
 * a dark system theme — notably an SVG logo that uses `prefers-color-scheme` to pick between a
 * black and white fill, which our SVG decoder doesn't evaluate, so it always renders the
 * non-media-query default (typically a black path on a transparent canvas). Compositing onto
 * solid white keeps the logo visible regardless of what surrounds it.
 */
fun Image.toOpaqueBitmap(backgroundColor: Int = android.graphics.Color.WHITE): Bitmap {
    val width = width.takeIf { it > 0 } ?: 512
    val height = height.takeIf { it > 0 } ?: 512
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    canvas.drawColor(backgroundColor)
    when {
        // A hardware bitmap can't be drawn into a software Canvas ("Software rendering doesn't
        // support hardware bitmaps") — copy() does the GPU-to-software readback instead.
        this is BitmapImage && this.bitmap.config == Bitmap.Config.HARDWARE ->
            canvas.drawBitmap(this.bitmap.copy(Bitmap.Config.ARGB_8888, false), 0f, 0f, null)
        this is BitmapImage -> canvas.drawBitmap(this.bitmap, 0f, 0f, null)
        else -> draw(canvas)
    }
    return bitmap
}
