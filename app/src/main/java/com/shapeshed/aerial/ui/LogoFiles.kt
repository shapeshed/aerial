package com.shapeshed.aerial.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import com.shapeshed.aerial.ArtworkProvider
import com.shapeshed.aerial.R
import android.webkit.MimeTypeMap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
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
import java.util.LinkedHashMap
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

internal fun mediaArtworkFileForSystem(file: File): File =
    if (file.extension.lowercase(Locale.US) == "svg") {
        File(file.parentFile, "${file.nameWithoutExtension}_media.png")
    } else file

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
 * Renders a Coil [Image] to a same-size ARGB bitmap, preserving its own transparency —
 * shared decode step behind [toOpaqueBitmap] and [isPredominantlyLight].
 */
private fun Image.toTransparentBitmap(): Bitmap {
    val width = width.takeIf { it > 0 } ?: 512
    val height = height.takeIf { it > 0 } ?: 512
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
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

/**
 * Rasterizes a Coil [Image] onto an opaque background. System media surfaces (quick controls,
 * lock screen, Bluetooth AVRCP, Android Auto) draw an artwork bitmap with no guaranteed
 * background behind it, so a source image with transparent areas can end up invisible against
 * a dark system theme — notably an SVG logo that uses `prefers-color-scheme` to pick between a
 * black and white fill, which our SVG decoder doesn't evaluate, so it always renders the
 * non-media-query default. A single fixed background makes one or the other invisible (#121),
 * so the backdrop contrasts with the logo's own [isPredominantlyLightBitmap] rather than
 * always being white — using the device's own dynamic-color neutral tones ([adaptiveNeutral])
 * rather than a fixed hardcoded color, matching the Compose UI's use of dynamic color, since
 * these surfaces have no MaterialTheme to draw from.
 */
fun Image.toOpaqueBitmap(context: Context): Bitmap {
    val content = toTransparentBitmap()
    val backgroundColor = context.adaptiveNeutral(dark = content.isPredominantlyLightBitmap())
    val bitmap = createBitmap(content.width, content.height)
    val canvas = Canvas(bitmap)
    canvas.drawColor(backgroundColor)
    canvas.drawBitmap(content, 0f, 0f, null)
    return bitmap
}

/**
 * A neutral tone from the device's Material You wallpaper-derived dynamic color palette
 * (Android 12+), falling back to a fixed near-black/white on older devices where dynamic
 * color doesn't exist.
 */
private fun Context.adaptiveNeutral(dark: Boolean): Int {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val resId = if (dark) android.R.color.system_neutral1_900 else android.R.color.system_neutral1_50
        return getColor(resId)
    }
    return if (dark) SYSTEM_SURFACE_DARK_PLATE_FALLBACK else android.graphics.Color.WHITE
}

/**
 * Whether an image's own rendered content reads as light-colored overall. Used to pick a
 * plate/backdrop that contrasts with the logo itself (light logo -> dark plate, dark logo ->
 * light plate) rather than a single fixed background that makes one of the two invisible
 * (#121).
 */
fun Image.isPredominantlyLight(): Boolean = toTransparentBitmap().isPredominantlyLightBitmap()

private fun Bitmap.isPredominantlyLightBitmap(): Boolean {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    var luminanceSum = 0.0
    var opaquePixelCount = 0
    for (pixel in pixels) {
        // Ignoring near-transparent pixels means a mostly-transparent SVG canvas doesn't
        // dilute the average toward "dark" regardless of its actual foreground color.
        if (android.graphics.Color.alpha(pixel) < MIN_OPAQUE_ALPHA) continue
        luminanceSum += 0.299 * android.graphics.Color.red(pixel) +
            0.587 * android.graphics.Color.green(pixel) +
            0.114 * android.graphics.Color.blue(pixel)
        opaquePixelCount++
    }
    return opaquePixelCount > 0 && (luminanceSum / opaquePixelCount) > MID_LUMINANCE
}

/** Strongly saturated, darker artwork benefits from a light plate in dark theme. */
private fun Image.prefersLightPlate(): Boolean = toTransparentBitmap().prefersLightPlate()

private fun Bitmap.prefersLightPlate(): Boolean {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    var vibrantCount = 0
    var opaqueCount = 0
    val hsv = FloatArray(3)
    for (pixel in pixels) {
        if (android.graphics.Color.alpha(pixel) < MIN_OPAQUE_ALPHA) continue
        opaqueCount++
        android.graphics.Color.colorToHSV(pixel, hsv)
        val luminance = 0.299 * android.graphics.Color.red(pixel) +
            0.587 * android.graphics.Color.green(pixel) +
            0.114 * android.graphics.Color.blue(pixel)
        if (hsv[1] >= MIN_VIBRANT_SATURATION && luminance < MAX_VIBRANT_LUMINANCE) {
            vibrantCount++
        }
    }
    return opaqueCount > 0 && vibrantCount.toFloat() / opaqueCount >= MIN_VIBRANT_FRACTION
}

private const val MIN_OPAQUE_ALPHA = 32
private const val MID_LUMINANCE = 127.5
private const val MIN_VIBRANT_SATURATION = 0.45f
private const val MAX_VIBRANT_LUMINANCE = 180.0
private const val MIN_VIBRANT_FRACTION = 0.20f

/**
 * Whether this image has any transparent margin of its own to justify an inset+plate
 * treatment. A full-bleed square "brand tile" logo (opaque corner to corner, common for
 * uploaded station artwork) is already complete artwork — insetting it just to reveal an
 * unwanted plate-colored border looks wrong; that treatment is only for icon-style artwork
 * drawn with its own transparent margin.
 */
fun Image.hasTransparentMargin(): Boolean {
    val bitmap = toTransparentBitmap()
    val right = bitmap.width - 1
    val bottom = bitmap.height - 1
    if (right < 0 || bottom < 0) return false
    return listOf(
        bitmap[0, 0],
        bitmap[right, 0],
        bitmap[0, bottom],
        bitmap[right, bottom],
    ).any { android.graphics.Color.alpha(it) < MIN_OPAQUE_ALPHA }
}

/** Detects circular artwork, including circular marks exported on an opaque square canvas. */
fun Image.hasCircularArtwork(): Boolean = toTransparentBitmap().hasCircularArtwork()

private fun Bitmap.hasCircularArtwork(): Boolean {
    if (width < 4 || height < 4) return false
    val insetX = (width * 0.08f).toInt().coerceAtLeast(1)
    val insetY = (height * 0.08f).toInt().coerceAtLeast(1)
    val corners = listOf(
        this[insetX, insetY],
        this[width - 1 - insetX, insetY],
        this[insetX, height - 1 - insetY],
        this[width - 1 - insetX, height - 1 - insetY],
    )
    val edges = listOf(
        this[width / 2, insetY],
        this[width - 1 - insetX, height / 2],
        this[width / 2, height - 1 - insetY],
        this[insetX, height / 2],
    )
    val transparentCorners = corners.count { android.graphics.Color.alpha(it) < MIN_OPAQUE_ALPHA }
    if (transparentCorners == corners.size) return edges.count { android.graphics.Color.alpha(it) >= MIN_OPAQUE_ALPHA } >= 2

    val cornerColor = corners.map { color ->
        floatArrayOf(
            android.graphics.Color.red(color) / 255f,
            android.graphics.Color.green(color) / 255f,
            android.graphics.Color.blue(color) / 255f,
        )
    }
    val cornersMatch = cornerColor.maxOf { color ->
        cornerColor.maxOf { other -> colorDistance(color, other) }
    } < 0.18f
    val averageCorner = FloatArray(3) { index -> cornerColor.map { it[index] }.average().toFloat() }
    val contrastingEdges = edges.count { color ->
        colorDistance(
            floatArrayOf(
                android.graphics.Color.red(color) / 255f,
                android.graphics.Color.green(color) / 255f,
                android.graphics.Color.blue(color) / 255f,
            ),
            averageCorner,
        ) > 0.20f
    }
    return cornersMatch && contrastingEdges >= 2
}

private fun colorDistance(first: FloatArray, second: FloatArray): Float =
    kotlin.math.sqrt(first.indices.sumOf { index ->
        val difference = first[index] - second[index]
        (difference * difference).toDouble()
    }).toFloat()


// MD3 baseline Neutral-10 (on-surface dark tone) — pre-API-31 fallback for adaptiveNeutral(),
// on devices with no dynamic color palette to draw from.
private const val SYSTEM_SURFACE_DARK_PLATE_FALLBACK = 0xFF1D1B20.toInt()
