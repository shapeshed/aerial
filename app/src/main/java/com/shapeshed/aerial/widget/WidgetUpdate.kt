package com.shapeshed.aerial.widget

import android.content.Context
import android.util.Log
import com.shapeshed.aerial.data.PlaybackSnapshotStore
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.ui.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

const val ACTION_WIDGET_PREVIOUS = "com.shapeshed.aerial.action.WIDGET_PREVIOUS"
const val ACTION_WIDGET_TOGGLE = "com.shapeshed.aerial.action.WIDGET_TOGGLE"
const val ACTION_WIDGET_NEXT = "com.shapeshed.aerial.action.WIDGET_NEXT"

internal data class WidgetNavigationAvailability(val previous: Boolean, val next: Boolean)

internal fun widgetNavigationAvailability(index: Int, size: Int): WidgetNavigationAvailability =
    WidgetNavigationAvailability(
        previous = index in 0 until size && size > 1,
        next = index in 0 until size && size > 1,
    )

/**
 * The widget's entry point for everything outside itself: redraw requests from playback, and the
 * play/pause/previous/next buttons.
 *
 * An interface so callers (`PlayerService`, `MainViewModel`, the receivers) depend on the
 * capability rather than on the database, DataStore and coroutine scope behind it.
 */
interface WidgetUpdater {
    /** Coalesces and schedules a redraw. Safe to call on every playback state change. */
    fun request()

    /** Performs a transport action, connecting to the media session for the duration. */
    suspend fun handlePlaybackAction(action: String?)
}

/**
 * Debounces redraws onto the application scope and drives the media session for widget buttons.
 *
 * Playback changes arrive in bursts (one metadata update fires several state writes), so requests
 * made within [UPDATE_DEBOUNCE_MS] collapse into a single redraw and a superseded request is
 * dropped before it does any work.
 */
class DefaultWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: StationRepository,
    private val snapshotStore: PlaybackSnapshotStore,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : WidgetUpdater {

    private val debounce = WidgetUpdateDebounce(UPDATE_DEBOUNCE_MS)

    override fun request() {
        applicationScope.launch {
            val token = debounce.awaitTurn() ?: return@launch
            try {
                updateAerialWidgets(context, repository, snapshotStore) { debounce.isCurrent(token) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Log.e(TAG, "Widget update failed", error)
            }
        }
    }

    override suspend fun handlePlaybackAction(action: String?) {
        handleWidgetPlaybackAction(context, repository, snapshotStore, action)
    }
}

private const val TAG = "AerialWidget"
private const val UPDATE_DEBOUNCE_MS = 150L
