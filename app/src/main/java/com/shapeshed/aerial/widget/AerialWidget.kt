package com.shapeshed.aerial.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.concurrent.futures.await
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.shapeshed.aerial.AerialApp
import com.shapeshed.aerial.PlayerService
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.toPlayableMediaItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val stationIdKey = ActionParameters.Key<Long>("station_id")

private val responsiveSizes = setOf(
    DpSize(180.dp, 110.dp),
    DpSize(250.dp, 180.dp),
    DpSize(320.dp, 250.dp),
)

internal fun stationsForWidget(stations: List<Station>): List<Station> = stations
    .asSequence()
    .filter(Station::isFavorite)
    .sortedBy { it.name.lowercase() }
    .toList()

class AerialWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(responsiveSizes)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as AerialApp
        val favorites = stationsForWidget(app.repository.getAll().first())
        val title = context.getString(com.shapeshed.aerial.R.string.widget_title)
        val emptyMessage = context.getString(com.shapeshed.aerial.R.string.widget_empty)
        val previous = context.getString(com.shapeshed.aerial.R.string.widget_previous)
        val play = context.getString(com.shapeshed.aerial.R.string.widget_play)
        val pause = context.getString(com.shapeshed.aerial.R.string.widget_pause)
        val next = context.getString(com.shapeshed.aerial.R.string.widget_next)
        val playback = readPlayback(context)

        provideContent {
            GlanceTheme {
                AerialWidgetContent(
                    favorites,
                    title,
                    emptyMessage,
                    previous,
                    if (playback.isPlaying) pause else play,
                    next,
                    playback,
                )
            }
        }
    }
}

private data class WidgetPlayback(
    val mediaId: String?,
    val isPlaying: Boolean,
)

private suspend fun readPlayback(context: Context): WidgetPlayback {
    val appContext = context.applicationContext
    return runCatching {
        val controller = MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, PlayerService::class.java)),
        ).buildAsync().await()
        try {
            WidgetPlayback(controller.currentMediaItem?.mediaId, controller.isPlaying)
        } finally {
            controller.release()
        }
    }.getOrDefault(WidgetPlayback(null, false))
}

@Composable
private fun AerialWidgetContent(
    favorites: List<Station>,
    title: String,
    emptyMessage: String,
    previous: String,
    playPause: String,
    next: String,
    playback: WidgetPlayback,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.Top,
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = previous,
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(18.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .clickable(actionRunCallback<PreviousFavoriteAction>()),
                style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer),
            )
            Spacer(GlanceModifier.size(6.dp))
            Text(
                text = playPause,
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(18.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable(actionRunCallback<TogglePlaybackAction>()),
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.size(6.dp))
            Text(
                text = next,
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(18.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .clickable(actionRunCallback<NextFavoriteAction>()),
                style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer),
            )
        }
        Spacer(GlanceModifier.size(8.dp))
        if (favorites.isEmpty()) {
            Text(
                text = emptyMessage,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                items(favorites.size) { index ->
                    val station = favorites[index]
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(
                                if (station.id.toString() == playback.mediaId) {
                                    GlanceTheme.colors.primaryContainer
                                } else {
                                    GlanceTheme.colors.surfaceVariant
                                },
                            )
                            .cornerRadius(20.dp)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .clickable(
                                actionRunCallback<PlayFavoriteAction>(
                                    actionParametersOf(stationIdKey to station.id),
                                ),
                            ),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                    ) {
                        Text(
                            text = station.name,
                            maxLines = 1,
                            style = TextStyle(
                                color = if (station.id.toString() == playback.mediaId) {
                                    GlanceTheme.colors.onPrimaryContainer
                                } else {
                                    GlanceTheme.colors.onSurfaceVariant
                                },
                            ),
                        )
                    }
                    Spacer(GlanceModifier.size(4.dp))
                }
            }
        }
    }
}

class PlayFavoriteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val stationId = parameters[stationIdKey] ?: return
        val app = context.applicationContext as AerialApp
        val station = app.repository.getById(stationId) ?: return
        if (!station.isFavorite) return
        val favorites = stationsForWidget(app.repository.getAll().first())
        val startIndex = favorites.indexOfFirst { it.id == stationId }.takeIf { it >= 0 } ?: return

        val appContext = context.applicationContext
        val controller = MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, PlayerService::class.java)),
        ).buildAsync().await()
        try {
            controller.setMediaItems(
                favorites.map { it.toPlayableMediaItem(appContext) },
                startIndex,
                androidx.media3.common.C.TIME_UNSET,
            )
            controller.prepare()
            controller.play()
        } finally {
            controller.release()
        }
        AerialWidget().update(context, glanceId)
    }
}

abstract class FavoriteNavigationAction : ActionCallback {
    protected suspend fun withController(
        context: Context,
        block: suspend (MediaController) -> Unit,
    ) {
        val appContext = context.applicationContext
        val controller = MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, PlayerService::class.java)),
        ).buildAsync().await()
        try {
            block(controller)
        } finally {
            controller.release()
        }
    }

    protected suspend fun navigateFavorites(context: Context, next: Boolean) {
        val app = context.applicationContext as AerialApp
        val favorites = stationsForWidget(app.repository.getAll().first())
        if (favorites.size < 2) return
        withController(context) { controller ->
            val currentIndex = favorites.indexOfFirst {
                it.id.toString() == controller.currentMediaItem?.mediaId
            }
            if (currentIndex < 0) return@withController
            controller.setMediaItems(
                favorites.map { it.toPlayableMediaItem(context.applicationContext) },
                currentIndex,
                androidx.media3.common.C.TIME_UNSET,
            )
            controller.prepare()
            if (next) controller.seekToNextMediaItem() else controller.seekToPreviousMediaItem()
            controller.play()
        }
    }
}

class PreviousFavoriteAction : FavoriteNavigationAction() {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        navigateFavorites(context, next = false)
        AerialWidget().update(context, glanceId)
    }
}

class NextFavoriteAction : FavoriteNavigationAction() {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        navigateFavorites(context, next = true)
        AerialWidget().update(context, glanceId)
    }
}

class TogglePlaybackAction : FavoriteNavigationAction() {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withController(context) { controller ->
            if (controller.isPlaying) controller.pause() else controller.play()
        }
        AerialWidget().update(context, glanceId)
    }
}

class AerialWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AerialWidget()

    override fun onReceive(context: Context, intent: android.content.Intent) {
        if (intent.action == ACTION_UPDATE_AERIAL_WIDGETS) {
            val pendingResult = goAsync()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                try {
                    AerialWidget().updateAll(context)
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            super.onReceive(context, intent)
        }
    }
}
