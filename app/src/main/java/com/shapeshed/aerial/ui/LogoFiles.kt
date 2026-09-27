package com.shapeshed.aerial.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.graphics.get
import coil3.Image
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.svg.SvgDecoder
import com.shapeshed.aerial.ArtworkProvider
import com.shapeshed.aerial.R
import com.shapeshed.aerial.playback.hasCircularArtwork
import com.shapeshed.aerial.playback.hasTransparentMargin
import com.shapeshed.aerial.playback.isPredominantlyLight
import com.shapeshed.aerial.playback.prefersLightPlate
import com.shapeshed.aerial.playback.toOpaqueBitmap
import java.io.File
import java.net.URL
import java.util.LinkedHashMap
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val ARTWORK_FETCH_TIMEOUT_MS = 3_000L

data class LogoAppearance(
    val isLight: Boolean,
    val hasTransparentMargin: Boolean,
    val prefersLightPlate: Boolean = false,
    val hasCircularArtwork: Boolean = false,
)

class LogoAppearanceCache(private val maxEntries: Int = 128) {
    init {
        require(maxEntries > 0) { "Logo appearance cache must hold at least one entry" }
    }

    private val entries = object : LinkedHashMap<String, LogoAppearance>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LogoAppearance>): Boolean =
            size > maxEntries
    }

    @Synchronized
    fun getOrCompute(key: String, compute: () -> LogoAppearance): LogoAppearance =
        entries[key] ?: compute().also { entries[key] = it }
}

class LogoAppearanceAnalyzer(
    private val cache: LogoAppearanceCache = LogoAppearanceCache(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend fun analyze(key: String, image: Image): LogoAppearance = withContext(dispatcher) {
        cache.getOrCompute(key) {
            LogoAppearance(
                isLight = image.isPredominantlyLight(),
                hasTransparentMargin = image.hasTransparentMargin(),
                prefersLightPlate = image.prefersLightPlate(),
                hasCircularArtwork = image.hasCircularArtwork(),
            )
        }
    }
}

val sharedLogoAppearanceAnalyzer = LogoAppearanceAnalyzer()

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

@Volatile private var localSvgLoader: ImageLoader? = null

private fun localSvgImageLoader(context: Context): ImageLoader =
    localSvgLoader ?: ImageLoader.Builder(context.applicationContext)
        .components { add(SvgDecoder.Factory()) }
        .build()
        .also { localSvgLoader = it }

/** Creates the bitmap companion required by system media consumers for a local SVG. */
suspend fun ensureMediaArtworkForLogo(context: Context, file: File): File {
    if (file.extension.lowercase(Locale.US) != "svg") return file

    val pngFile = mediaArtworkFileForSystem(file)
    if (pngFile.exists()) return pngFile

    return try {
        val request = ImageRequest.Builder(context).data(file).size(512).build()
        val result = localSvgImageLoader(context).execute(request) as? SuccessResult ?: return file
        val bitmap = result.image.toOpaqueBitmap(context)
        pngFile.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
        pngFile
    } catch (error: CancellationException) {
        throw error
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

    val cacheDir = File(context.cacheDir, ArtworkProvider.REGISTRY_ARTWORK_DIR)
    val pngFile = File(cacheDir, "${logoUrl.hashCode().toUInt()}.png")
    if (pngFile.exists()) {
        return ArtworkProvider.uriFor(context, ArtworkProvider.REGISTRY_ARTWORK_DIR, pngFile.name)
    }

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
        val bitmap = result.image.toOpaqueBitmap(context)
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
        ArtworkProvider.uriFor(context, ArtworkProvider.REGISTRY_ARTWORK_DIR, pngFile.name)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    }
}

/**
 * A stable content:// URI for a favourited station's locally-cached logo file (already on disk
 * under filesDir/logos — see [ArtworkLoader]). Media3's Coil bitmap loader can decode the
 * original SVG directly, so custom artwork does not need a second rasterized file beside it.
 * Keeping artwork as a URI also avoids embedding a Bitmap in every queue item, which is what
 * overloads Bluetooth AVRCP queue-diffing (#123).
 */
fun localLogoArtworkUri(context: Context, file: File): Uri? {
    val artworkFile = mediaArtworkFileForSystem(file)
    if (!artworkFile.exists()) return null
    return ArtworkProvider.uriFor(context, ArtworkProvider.LOCAL_LOGO_DIR, artworkFile.name)
}

internal fun mediaArtworkFileForSystem(file: File): File = if (file.extension.lowercase(Locale.US) == "svg") {
    File(file.parentFile, "${file.nameWithoutExtension}_media.png")
} else {
    file
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

private fun extensionFromMimeType(mimeType: String?): String? = mimeType?.let {
    MimeTypeMap.getSingleton()
        .getExtensionFromMimeType(it)
        ?.lowercase(Locale.US)
}

private fun String.extensionOrNull(): String? = substringAfterLast('.', missingDelimiterValue = "")
    .lowercase(Locale.US)
    .takeIf { it.isNotBlank() && it.length <= 5 }
