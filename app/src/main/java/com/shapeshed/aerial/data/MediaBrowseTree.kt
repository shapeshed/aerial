package com.shapeshed.aerial.data

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.shapeshed.aerial.R
import com.shapeshed.aerial.REGISTRY_MEDIA_ID_PREFIX
import com.shapeshed.aerial.toBrowseMediaItem
import com.shapeshed.aerial.toPlayableMediaItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/** Runs [transform] for all items concurrently — browse lists resolve per-station artwork that
 * may each hit the network (see cachedRemoteArtworkUri), and doing that sequentially would
 * stack timeouts into a folder-level stall Android Auto gives up on. */
private suspend fun <T, R> List<T>.mapConcurrently(transform: suspend (T) -> R): List<R> =
    coroutineScope { map { async { transform(it) } }.awaitAll() }

const val MEDIA_BROWSE_ROOT_ID = "root"
private const val FAVORITES_ID = "favorites"
private const val MOODS_ID = "moods"
const val RECENT_ID = "recent"
private const val MOOD_ID_PREFIX = "mood:"
private const val RECENT_LIMIT = 10

private val MOOD_TITLES = listOf(
    "relax" to R.string.mood_relax,
    "focus" to R.string.mood_focus,
    "morning" to R.string.mood_morning,
    "driving" to R.string.mood_driving,
    "late_night" to R.string.mood_late_night,
    "workout" to R.string.mood_workout,
)

/**
 * Builds the media browse tree shared by any surface that browses Aerial's content through a
 * MediaLibraryService (Android Auto today, Google TV planned) — plain suspend functions, no
 * session/callback types, so it's unit-testable the same way the rest of the data layer is.
 *
 * Tree shape: root -> Favorites | Moods | Recently Played, Moods -> one folder per curated mood.
 * Search is a separate root capability, not a tab (see [search]).
 */
class MediaBrowseTree(
    private val context: Context,
    private val stationRepository: StationRepository,
    private val registryRepository: RegistryRepository,
) {
    fun rootItem(): MediaItem = browsableFolder(MEDIA_BROWSE_ROOT_ID, context.getString(R.string.app_name))

    fun rootChildren(): List<MediaItem> = listOf(
        browsableFolder(FAVORITES_ID, context.getString(R.string.tab_favorites)),
        browsableFolder(MOODS_ID, context.getString(R.string.car_moods)),
        browsableFolder(RECENT_ID, context.getString(R.string.recently_played)),
    )

    /** Dispatches a parentId to the right children list, or null if it's not a known folder. */
    suspend fun children(parentId: String): List<MediaItem>? = when {
        parentId == MEDIA_BROWSE_ROOT_ID -> rootChildren()
        parentId == FAVORITES_ID -> favoriteChildren()
        parentId == MOODS_ID -> moodChildren()
        parentId == RECENT_ID -> recentChildren()
        parentId.startsWith(MOOD_ID_PREFIX) -> moodStationChildren(parentId.removePrefix(MOOD_ID_PREFIX))
        else -> null
    }

    suspend fun favoriteChildren(): List<MediaItem> =
        stationRepository.getAll().first().mapConcurrently { it.toBrowseMediaItem(context) }

    fun moodChildren(): List<MediaItem> = MOOD_TITLES.map { (id, titleRes) ->
        browsableFolder("$MOOD_ID_PREFIX$id", context.getString(titleRes))
    }

    suspend fun moodStationChildren(moodId: String): List<MediaItem> =
        registryRepository.curatedMoodStations()[moodId]
            ?.mapConcurrently { it.toPlayableMediaItem(context) }
            ?: emptyList()

    // Sourced from play_history (any station played, favorited or not) rather than favorited
    // stations' lastPlayedAt, so browsing a mood/search result shows up here even if it's never
    // been saved. Entries only resolve for registry-backed stations (provider+providerId); a
    // history row whose station has since vanished from the registry — or a curated mood
    // station without a real providerId — is silently skipped rather than shown broken.
    suspend fun recentChildren(): List<MediaItem> =
        stationRepository.recentlyPlayed(RECENT_LIMIT)
            .mapNotNull { entry -> registryRepository.getByProviderId(entry.provider, entry.providerId) }
            .mapConcurrently { it.toPlayableMediaItem(context) }

    /** Broad catalogue search (not just favorites) so voice search can find and play any station
     * in the registry, matching what "Hey Google, play X on Aerial" needs. */
    suspend fun search(query: String): List<MediaItem> =
        registryRepository.search(query).mapConcurrently { it.toPlayableMediaItem(context) }

    /**
     * Resolves a bare mediaId — from a browse-item tap, voice search, or playback resumption —
     * into a fully playable [MediaItem]. Used by `onGetItem` and `onSetMediaItems`: Android
     * Auto's legacy MediaBrowserCompat bridge dispatches "play this" by mediaId string alone, not
     * by replaying the exact MediaItem object the browse tree returned, so the session must be
     * able to look any mediaId up again here.
     */
    suspend fun resolve(mediaId: String): MediaItem? {
        if (mediaId.startsWith(REGISTRY_MEDIA_ID_PREFIX)) {
            val registryId = mediaId.removePrefix(REGISTRY_MEDIA_ID_PREFIX).toLongOrNull() ?: return null
            return registryRepository.getById(registryId)?.toPlayableMediaItem(context)
        }
        val id = mediaId.toLongOrNull() ?: return null
        // toPlayableMediaItem, not toBrowseMediaItem: this sits on the playback-start path
        // (onSetMediaItems), and the session's own artwork loads through CoilBitmapLoader —
        // proxying the logo here would block "tap play" on an uncached network fetch.
        return stationRepository.getById(id)?.toPlayableMediaItem(context)
    }

    private fun browsableFolder(id: String, title: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
            .build()
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(metadata)
            .build()
    }
}
