package com.shapeshed.aerial

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession

/** Media3 queue behavior shared by phone, notification, Bluetooth, and Auto controllers. */
@OptIn(UnstableApi::class)
internal fun createSessionPlayer(player: Player): Player = object : ForwardingPlayer(player) {
    override fun seekToPrevious() = seekToPreviousMediaItem()
    override fun seekToNext() = seekToNextMediaItem()

    override fun isCommandAvailable(command: Int): Boolean =
        command.isSkipCommandAvailableFor(player.mediaItemCount) && super.isCommandAvailable(command)

    override fun getAvailableCommands(): Player.Commands =
        super.getAvailableCommands().withoutSkipCommandsFor(player.mediaItemCount)
}

private fun Int.isSkipCommandAvailableFor(mediaItemCount: Int): Boolean = mediaItemCount > 1 || this !in SKIP_COMMANDS

@OptIn(UnstableApi::class)
private fun Player.Commands.withoutSkipCommandsFor(mediaItemCount: Int): Player.Commands =
    if (mediaItemCount > 1) this else buildUpon().removeAll(*SKIP_COMMANDS).build()

private val SKIP_COMMANDS = intArrayOf(
    Player.COMMAND_SEEK_TO_PREVIOUS,
    Player.COMMAND_SEEK_TO_NEXT,
    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
)

/** `ControllerInfo.getControllerVersion()` of a platform-session (MediaController) caller. */
private const val LEGACY_CONTROLLER_VERSION = 0

private val TRANSPORT_COMMANDS = intArrayOf(
    Player.COMMAND_PLAY_PAUSE,
    Player.COMMAND_PREPARE,
    Player.COMMAND_STOP,
    *SKIP_COMMANDS,
)

/**
 * Player commands to grant a connecting controller.
 *
 * Media3 gives an untrusted controller read-only commands, which silently drops play, pause and
 * skip. Garmin Connect (and other watch/headset bridges) reach the session as a platform
 * MediaController, and Aerial cannot see their package, so they are never "trusted". Such legacy
 * controllers are only able to attach with user-granted notification or media-control access, so
 * give them transport controls (and nothing that changes the queue). Untrusted Media3 controllers
 * stay read-only.
 */
@OptIn(UnstableApi::class)
internal fun availablePlayerCommandsFor(isTrusted: Boolean, controllerVersion: Int): Player.Commands = when {
    isTrusted -> MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS

    grantsTransportCommands(isTrusted, controllerVersion) ->
        MediaSession.ConnectionResult.DEFAULT_UNTRUSTED_PLAYER_COMMANDS
            .buildUpon()
            .addAll(*TRANSPORT_COMMANDS)
            .build()

    else -> MediaSession.ConnectionResult.DEFAULT_UNTRUSTED_PLAYER_COMMANDS
}

/** True for an untrusted platform-session controller, which gets transport commands. */
internal fun grantsTransportCommands(isTrusted: Boolean, controllerVersion: Int): Boolean =
    !isTrusted && controllerVersion == LEGACY_CONTROLLER_VERSION

/** Expands an external controller's single browse item into its navigable sibling queue. */
@OptIn(UnstableApi::class)
internal suspend fun expandControllerQueue(
    mediaItems: List<MediaItem>,
    startIndex: Int,
    startPositionMs: Long,
    controllerPackage: String,
    appPackage: String,
    parentIdForMediaId: (String) -> String?,
    childrenForParent: suspend (String) -> List<MediaItem>?,
    resolveMediaItem: suspend (String) -> MediaItem?,
): MediaSession.MediaItemsWithStartPosition {
    val effectiveIndex = startIndex.takeIf { it in mediaItems.indices } ?: 0
    val tappedId = mediaItems.getOrNull(effectiveIndex)?.mediaId
    val parentId = tappedId
        ?.takeIf { mediaItems.size == 1 && controllerPackage != appPackage }
        ?.let(parentIdForMediaId)
    val siblings = if (parentId != null) childrenForParent(parentId) else null
    val siblingIndex = siblings?.indexOfFirst { it.mediaId == tappedId } ?: -1
    return if (siblings != null && siblingIndex >= 0) {
        MediaSession.MediaItemsWithStartPosition(siblings, siblingIndex, startPositionMs)
    } else {
        val resolved = mediaItems.map { item ->
            resolveMediaItem(item.mediaId)
                ?: item.takeIf { controllerPackage == appPackage }
                ?: throw IllegalArgumentException("Unresolved media item: ${item.mediaId}")
        }
        MediaSession.MediaItemsWithStartPosition(resolved, startIndex, startPositionMs)
    }
}
