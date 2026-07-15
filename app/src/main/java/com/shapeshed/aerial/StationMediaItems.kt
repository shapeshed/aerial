package com.shapeshed.aerial

import android.content.Context
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.bauerStreamUrl
import com.shapeshed.aerial.ui.appIconBitmap
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
 * surfaces describe a station identically.
 */
fun stationMediaMetadata(context: Context, station: Station): MediaMetadata {
    val artworkUri = station.logoPath
        .takeIf { it.startsWith("http") }
        ?.toUri()
    val localArtworkData = station.logoPath
        .takeIf { it.isNotEmpty() && !it.startsWith("http") }
        ?.let { compressedLogoData(File(it)) }

    return MediaMetadata.Builder().apply {
        when {
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
        })
        .build()
}

/** A fully resolved, directly playable [MediaItem] — real stream URI included, no further
 * resolution needed by whatever surface (phone controller, Android Auto, Google TV) plays it. */
fun Station.toPlayableMediaItem(context: Context): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(bauerStreamUrl(this))
        .setMediaMetadata(stationMediaMetadata(context, this))
        .build()

/** Prefix marking a browse-tree mediaId as a registry (not-yet-saved) station, keyed by
 * [RegistryStation.id] rather than the shared `0` every ephemeral [Station] otherwise has —
 * multiple registry stations appear side by side in mood/search browse results, so their
 * mediaIds must stay distinct from each other and from real saved stations. */
const val REGISTRY_MEDIA_ID_PREFIX = "reg:"

/** A fully resolved, directly playable [MediaItem] for a browse-tree leaf sourced from the
 * station registry (mood/search results) — see [REGISTRY_MEDIA_ID_PREFIX]. */
fun RegistryStation.toPlayableMediaItem(context: Context): MediaItem {
    val ephemeral = toEphemeralStation()
    return MediaItem.Builder()
        .setMediaId("$REGISTRY_MEDIA_ID_PREFIX$id")
        .setUri(bauerStreamUrl(ephemeral))
        .setMediaMetadata(stationMediaMetadata(context, ephemeral))
        .build()
}
