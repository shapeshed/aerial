package com.shapeshed.aerial.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.graphics.get
import coil3.Image
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.svg.SvgDecoder
import com.shapeshed.aerial.playback.hasCircularArtwork
import com.shapeshed.aerial.playback.hasTransparentMargin
import com.shapeshed.aerial.playback.isPredominantlyLight
import com.shapeshed.aerial.playback.mediaArtworkFileForSystem
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

private fun extensionFromMimeType(mimeType: String?): String? = mimeType?.let {
    MimeTypeMap.getSingleton()
        .getExtensionFromMimeType(it)
        ?.lowercase(Locale.US)
}

private fun String.extensionOrNull(): String? = substringAfterLast('.', missingDelimiterValue = "")
    .lowercase(Locale.US)
    .takeIf { it.isNotBlank() && it.length <= 5 }
