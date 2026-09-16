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

        provideContent {
            GlanceTheme {
                AerialWidgetContent(favorites, title, emptyMessage)
            }
        }
    }
}

@Composable
private fun AerialWidgetContent(
    favorites: List<Station>,
    title: String,
    emptyMessage: String,
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
                            .background(GlanceTheme.colors.surfaceVariant)
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
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
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

        val appContext = context.applicationContext
        val controller = MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, PlayerService::class.java)),
        ).buildAsync().await()
        try {
            controller.setMediaItem(station.toPlayableMediaItem(appContext))
            controller.prepare()
            controller.play()
        } finally {
            controller.release()
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
