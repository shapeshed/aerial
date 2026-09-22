package com.shapeshed.aerial.widget

import android.content.Context
import android.content.SharedPreferences

internal data class WidgetPlaybackState(
    val mediaId: String?,
    val isPlaying: Boolean,
    val trackTitle: String?,
    val trackArtist: String?,
    val canSkipPrevious: Boolean,
    val canSkipNext: Boolean,
)

/**
 * Persists the widget's playback state. The [Context] overloads are the production surface; the
 * [SharedPreferences] overloads are the seam JVM tests drive directly, mirroring the widget's
 * observable behaviour without an Android runtime.
 */
internal object WidgetPlaybackStore {
    private const val PREFERENCES = "aerial_widget_playback"
    private const val MEDIA_ID = "media_id"
    private const val IS_PLAYING = "is_playing"
    private const val TRACK_TITLE = "track_title"
    private const val TRACK_ARTIST = "track_artist"
    private const val CAN_SKIP_PREVIOUS = "can_skip_previous"
    private const val CAN_SKIP_NEXT = "can_skip_next"

    fun read(context: Context): WidgetPlaybackState = read(preferences(context))

    internal fun read(preferences: SharedPreferences): WidgetPlaybackState = WidgetPlaybackState(
        mediaId = preferences.getString(MEDIA_ID, null),
        isPlaying = preferences.getBoolean(IS_PLAYING, false),
        trackTitle = preferences.getString(TRACK_TITLE, null),
        trackArtist = preferences.getString(TRACK_ARTIST, null),
        canSkipPrevious = preferences.getBoolean(CAN_SKIP_PREVIOUS, false),
        canSkipNext = preferences.getBoolean(CAN_SKIP_NEXT, false),
    )

    fun write(context: Context, mediaId: String?, isPlaying: Boolean, canSkipPrevious: Boolean, canSkipNext: Boolean) =
        write(preferences(context), mediaId, isPlaying, canSkipPrevious, canSkipNext)

    internal fun write(
        preferences: SharedPreferences,
        mediaId: String?,
        isPlaying: Boolean,
        canSkipPrevious: Boolean,
        canSkipNext: Boolean,
    ) {
        val stationChanged = preferences.getString(MEDIA_ID, null) != mediaId
        preferences
            .edit()
            .apply {
                if (mediaId == null) remove(MEDIA_ID) else putString(MEDIA_ID, mediaId)
                putBoolean(IS_PLAYING, isPlaying)
                putBoolean(CAN_SKIP_PREVIOUS, canSkipPrevious)
                putBoolean(CAN_SKIP_NEXT, canSkipNext)
                if (stationChanged) {
                    remove(TRACK_TITLE)
                    remove(TRACK_ARTIST)
                }
            }
            .apply()
    }

    fun writeMetadata(context: Context, mediaId: String?, title: String?, artist: String?) =
        writeMetadata(preferences(context), mediaId, title, artist)

    internal fun writeMetadata(preferences: SharedPreferences, mediaId: String?, title: String?, artist: String?) {
        preferences
            .edit()
            .apply {
                if (mediaId == null) remove(MEDIA_ID) else putString(MEDIA_ID, mediaId)
                if (title.isNullOrBlank()) remove(TRACK_TITLE) else putString(TRACK_TITLE, title)
                if (artist.isNullOrBlank()) remove(TRACK_ARTIST) else putString(TRACK_ARTIST, artist)
            }
            .apply()
    }

    fun markStopped(context: Context) = markStopped(preferences(context))

    internal fun markStopped(preferences: SharedPreferences) {
        preferences
            .edit()
            .putBoolean(IS_PLAYING, false)
            .apply()
    }

    private fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
