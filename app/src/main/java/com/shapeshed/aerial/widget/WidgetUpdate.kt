package com.shapeshed.aerial.widget

import android.content.Context
import android.util.Log
import com.shapeshed.aerial.data.PlaybackSnapshotStore
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.di.ApplicationScope
import com.shapeshed.aerial.playback.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

const val ACTION_WIDGET_PREVIOUS = "com.shapeshed.aerial.action.WIDGET_PREVIOUS"
const val ACTION_WIDGET_TOGGLE = "com.shapeshed.aerial.action.WIDGET_TOGGLE"
const val ACTION_WIDGET_NEXT = "com.shapeshed.aerial.action.WIDGET_NEXT"

/**
 * Debounces redraws onto the application scope and drives the media session for widget buttons.
 *
 * Stays in `widget` rather than moving next to [WidgetUpdater] because the redraw needs
 * `RemoteViews` and `AppWidgetManager`; the interface it implements is the part other packages
 * depend on.
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
