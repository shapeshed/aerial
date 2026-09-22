package com.shapeshed.aerial

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.shapeshed.aerial.data.MediaBrowseTree
import com.shapeshed.aerial.data.PlaybackSnapshotStore
import com.shapeshed.aerial.data.StationArtworkResolver
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.data.queueForResumption
import com.shapeshed.aerial.data.resolveQueueStart
import kotlinx.coroutines.flow.first

/**
 * Produces the media items and library results a Media3 session serves to Android
 * Auto / Google TV and to reconnecting controllers. Extracted from PlayerService so
 * the browse and queue-expansion logic is not tangled with player lifecycle.
 */
@OptIn(UnstableApi::class)
internal class PlaybackSessionCoordinator(
    private val context: Context,
    private val browseTree: MediaBrowseTree,
    private val repository: StationRepository,
    private val snapshotStore: PlaybackSnapshotStore,
    private val artworkResolver: StationArtworkResolver,
) {
    // Remembered per mediaId (not just "whichever folder was browsed last") so a tap on one of
    // these (via onSetMediaItems) can queue the tapped item's whole folder, giving Android Auto
    // skip next/previous and an Up Next queue between stations instead of a single-item timeline.
    // Auto prefetches sibling folders' contents in the background, so a single last-folder
    // variable would be clobbered before the user taps play; only the mediaId -> folder mapping
    // is kept, and the folder's contents are rebuilt fresh at play time.
    private val parentIdByMediaId = mutableMapOf<String, String>()

    fun libraryRoot(): LibraryResult<MediaItem> {
        // Folders (Favorites/Moods/Recently Played) read as a list; station logos read well as a
        // grid, similar to most radio/podcast apps on Android Auto.
        val rootExtras = Bundle().apply {
            putInt(
                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
            )
            putInt(
                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
            )
        }
        val rootParams = LibraryParams.Builder().setExtras(rootExtras).build()
        return LibraryResult.ofItem(browseTree.rootItem(), rootParams)
    }

    suspend fun resolveItem(mediaId: String): LibraryResult<MediaItem> =
        browseTree.resolve(mediaId)?.let { LibraryResult.ofItem(it, null) }
            ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)

    suspend fun children(
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): LibraryResult<ImmutableList<MediaItem>> {
        val children = browseTree.children(parentId)
            ?: return LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        if (children.isNotEmpty() && children.all { it.mediaMetadata.isPlayable == true }) {
            children.forEach { parentIdByMediaId[it.mediaId] = parentId }
        }
        return LibraryResult.ofItemList(children.paginated(page, pageSize), params)
    }

    suspend fun searchCount(query: String): Int = browseTree.search(query).size

    suspend fun searchResults(
        query: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): LibraryResult<ImmutableList<MediaItem>> =
        LibraryResult.ofItemList(browseTree.search(query).paginated(page, pageSize), params)

    // Android Auto's legacy MediaBrowserCompat bridge plays a tapped browse item (or a voice
    // search/resumption result) by dispatching a MediaItem carrying only a mediaId, not the fully
    // resolved item the browse tree returned, so it must be looked up again before ExoPlayer can
    // play it. Falls back to the incoming item unchanged if it doesn't resolve.
    suspend fun setMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
        controllerPackage: String,
    ): MediaSession.MediaItemsWithStartPosition = expandControllerQueue(
        mediaItems = mediaItems,
        startIndex = startIndex,
        startPositionMs = startPositionMs,
        controllerPackage = controllerPackage,
        appPackage = context.packageName,
        parentIdForMediaId = { parentIdByMediaId[it] },
        childrenForParent = { browseTree.children(it) },
        resolveMediaItem = { browseTree.resolve(it) },
    )

    // Called when a controller (lock-screen/notification, Bluetooth, Assistant) reconnects to a
    // session whose player has no media item, e.g. the process was killed while the screen was off
    // and the system is restarting the service for a media button. Without this, that reconnection
    // carries only the single cached item, so Previous/Next have nothing to navigate.
    suspend fun playbackResumption(): MediaSession.MediaItemsWithStartPosition {
        val snapshot = snapshotStore.read()
            ?: throw UnsupportedOperationException("No last-played station to resume")
        val savedStation = snapshot.station.id.takeIf { it > 0 }?.let { repository.getById(it) }
            ?: repository.getByStreamUrl(snapshot.station.streamUrl)
        val queue = queueForResumption(snapshot.queue, repository.getAll().first(), snapshotStore.favoriteSort())
        val resumed = savedStation ?: snapshot.station.copy(id = 0)
        val startIndex = resolveQueueStart(queue, resumed)
        return if (startIndex != null) {
            MediaSession.MediaItemsWithStartPosition(
                queue.map { artworkResolver.recover(it).toSystemPlayableMediaItem(context) },
                startIndex,
                C.TIME_UNSET,
            )
        } else {
            MediaSession.MediaItemsWithStartPosition(
                listOf(artworkResolver.recover(resumed).toSystemPlayableMediaItem(context)),
                0,
                C.TIME_UNSET,
            )
        }
    }

    private fun List<MediaItem>.paginated(page: Int, pageSize: Int): List<MediaItem> {
        if (pageSize <= 0) return this
        val from = (page * pageSize).coerceIn(0, size)
        val to = (from + pageSize).coerceIn(from, size)
        return subList(from, to)
    }
}
