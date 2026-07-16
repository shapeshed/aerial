package com.shapeshed.aerial

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.bauerStreamUrl
import com.shapeshed.aerial.ui.appIconBitmap
import com.shapeshed.aerial.ui.cachedRemoteArtworkUri
import com.shapeshed.aerial.ui.compressedLogoData
import java.io.File

/**
 * A registry catalogue result not yet saved locally, mirrored as an ephemeral (id=0) [Station] so
 * it can be played and displayed the same way as a saved one.
 */
fun RegistryStation.toEphemeralStation(): Station = Station(
    name = name,
    streamUrl = streamUrl,
    logoPath = logoUrl,
    provider = provider,
    providerId = providerId,
    tags = tags,
    description = description,
    country = country,
    countryCode = countryCode,
)

/**
 * Shared by direct phone playback and the media browse tree (Android Auto, Google TV), so both
 * surfaces describe a station identically. [artworkUriOverride] substitutes a decodable
 * content:// URI (a registry SVG rasterized to PNG, served by [ArtworkProvider]) for surfaces
 * that decode a MediaItem's artwork themselves and can't handle SVG, like Android Auto's
 * browse lists.
 */
fun stationMediaMetadata(
    context: Context,
    station: Station,
    artworkUriOverride: Uri? = null,
): MediaMetadata {
    val artworkUri = station.logoPath
        .takeIf { it.startsWith("http") }
        ?.toUri()
    val localArtworkData = station.logoPath
        .takeIf { it.isNotEmpty() && !it.startsWith("http") }
        ?.let { compressedLogoData(File(it)) }

    return MediaMetadata.Builder().apply {
        when {
            artworkUriOverride != null -> setArtworkUri(artworkUriOverride)
            localArtworkData != null -> setArtworkData(
                localArtworkData,
                MediaMetadata.PICTURE_TYPE_FRONT_COVER,
            )
            artworkUri != null -> setArtworkUri(artworkUri)
            else -> appIconBitmap(context)?.let {
                setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }
        }
    }
        .setTitle(station.name)
        .setArtist(context.getString(R.string.live_radio))
        .setSubtitle(context.getString(R.string.live_radio))
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
        .setExtras(Bundle().apply {
            putString("provider", station.provider)
            putString("providerId", station.providerId)
            putString("streamUrl", station.streamUrl)
            putString("logoPath", station.logoPath)
        })
        .build()
}

/** A fully resolved, directly playable [MediaItem] — real stream URI included, no further
 * resolution needed by whatever surface (phone controller, Android Auto, Google TV) plays it. */
fun Station.toPlayableMediaItem(context: Context, artworkUriOverride: Uri? = null): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(bauerStreamUrl(this))
        .setMediaMetadata(stationMediaMetadata(context, this, artworkUriOverride))
        .build()

/** [toPlayableMediaItem] for the media browse tree: suspends to proxy a remote logo Android
 * Auto can't fetch or decode itself (SVG or cleartext http) through [ArtworkProvider] — see
 * [cachedRemoteArtworkUri]. Phone playback keeps the plain variant; its surfaces load artwork
 * through the app's own Coil stack, which handles both. */
suspend fun Station.toBrowseMediaItem(context: Context): MediaItem =
    toPlayableMediaItem(context, cachedRemoteArtworkUri(context, logoPath))

/** Prefix marking a browse-tree mediaId as a registry (not-yet-saved) station, keyed by
 * [RegistryStation.id] rather than the shared `0` every ephemeral [Station] otherwise has —
 * multiple registry stations appear side by side in mood/search browse results, so their
 * mediaIds must stay distinct from each other and from real saved stations. */
const val REGISTRY_MEDIA_ID_PREFIX = "reg:"

/** A fully resolved, directly playable [MediaItem] for a browse-tree leaf sourced from the
 * station registry (mood/search results) — see [REGISTRY_MEDIA_ID_PREFIX]. Suspends to
 * rasterize an SVG logo to a PNG-backed content URI (cached on disk), since browse-list
 * consumers like Android Auto can't decode SVG from a remote artworkUri themselves. */
suspend fun RegistryStation.toPlayableMediaItem(context: Context): MediaItem {
    val ephemeral = toEphemeralStation()
    val artworkUriOverride = cachedRemoteArtworkUri(context, logoUrl)
    return MediaItem.Builder()
        .setMediaId("$REGISTRY_MEDIA_ID_PREFIX$id")
        .setUri(bauerStreamUrl(ephemeral))
        .setMediaMetadata(stationMediaMetadata(context, ephemeral, artworkUriOverride))
        .build()
}
