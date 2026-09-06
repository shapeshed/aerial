package com.shapeshed.aerial

import androidx.media3.common.MediaItem
import com.shapeshed.aerial.data.Station

/** Resolves a Media3 item to a saved station or reconstructs an ephemeral station. */
internal fun stationFromMediaItem(mediaItem: MediaItem?, stations: List<Station>): Station? {
    if (mediaItem == null) return null
    mediaItem.mediaId.toLongOrNull()?.let { id ->
        stations.firstOrNull { it.id == id }?.let { return it }
    }
    val extras = mediaItem.mediaMetadata.extras ?: return null
    val streamUrl = extras.getString("streamUrl")?.takeIf { it.isNotBlank() } ?: return null
    val provider = extras.getString("provider").orEmpty()
    val providerId = extras.getString("providerId").orEmpty()
    if (provider.isNotBlank() && providerId.isNotBlank()) {
        stations.firstOrNull { it.provider == provider && it.providerId == providerId }?.let { return it }
    }
    stations.firstOrNull { it.streamUrl == streamUrl }?.let { return it }
    return Station(
        id = 0,
        name = stationNameFromMediaMetadata(
            extras.getString("stationName"),
            mediaItem.mediaMetadata.title,
        ),
        streamUrl = streamUrl,
        logoPath = extras.getString("logoPath").orEmpty(),
        provider = provider,
        providerId = providerId,
    )
}
