package com.shapeshed.aerial.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SizeF
import android.widget.RemoteViews
import androidx.concurrent.futures.await
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.shapeshed.aerial.AerialApp
import com.shapeshed.aerial.MainActivity
import com.shapeshed.aerial.PlayerService
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.PlaybackSnapshotStore
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.toPlayableMediaItem
import com.shapeshed.aerial.toSystemPlayableMediaItem
import com.shapeshed.aerial.ui.computeTrackDisplay
import com.shapeshed.aerial.ui.hasCircularArtwork
import com.shapeshed.aerial.ui.toTransparentBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal fun stationsForWidget(stations: List<Station>): List<Station> = stations
    .asSequence()
    .filter(Station::isFavorite)
    .sortedBy { it.name.lowercase() }
    .toList()

internal suspend fun updateAerialWidgets(
    context: Context,
    shouldPublish: () -> Boolean = { true },
) {
    val app = context.applicationContext as AerialApp
    val favorites = stationsForWidget(app.repository.getAll().first())
    val playback = WidgetPlaybackStore.read(app)
    val station = selectedStation(app, favorites, playback.mediaId)
    val artwork = station?.let { stationArtwork(app, it) }
    val playbackDisplay = station?.let {
        computeTrackDisplay(
            stationName = it.name,
            trackTitle = playback.trackTitle?.takeIf(::isMeaningfulWidgetMetadata),
            trackArtist = playback.trackArtist?.takeIf(::isMeaningfulWidgetMetadata),
            liveRadio = app.getString(R.string.live_radio),
        )
    }
    val layouts = mapOf(
        widgetSize(WIDGET_NARROW_WIDTH_DP, WIDGET_STICK_HEIGHT_DP) to widgetViews(
            app,
            R.layout.widget_player_stick,
            station,
            artwork,
            playbackDisplay?.title,
            playbackDisplay?.artist,
            playback,
            hasArtwork = false,
            hasText = false,
        ),
        widgetSize(WIDGET_WIDE_WIDTH_DP, WIDGET_STICK_HEIGHT_DP) to widgetViews(
            app,
            R.layout.widget_player_stick,
            station,
            artwork,
            playbackDisplay?.title,
            playbackDisplay?.artist,
            playback,
            hasArtwork = false,
            hasText = false,
        ),
        widgetSize(WIDGET_NARROW_WIDTH_DP, WIDGET_WAFER_HEIGHT_DP) to widgetViews(
            app,
            R.layout.widget_player_narrow,
            station,
            artwork,
            playbackDisplay?.title,
            playbackDisplay?.artist,
            playback,
            hasText = false,
        ),
        widgetSize(WIDGET_WIDE_WIDTH_DP, WIDGET_WAFER_HEIGHT_DP) to widgetViews(
            app,
            R.layout.widget_player_narrow,
            station,
            artwork,
            playbackDisplay?.title,
            playbackDisplay?.artist,
            playback,
            hasText = false,
        ),
        widgetSize(WIDGET_NARROW_WIDTH_DP, WIDGET_TALL_HEIGHT_DP) to widgetViews(
            app,
            R.layout.widget_player,
            station,
            artwork,
            playbackDisplay?.title,
            playbackDisplay?.artist,
            playback,
        ),
        widgetSize(WIDGET_WIDE_WIDTH_DP, WIDGET_TALL_HEIGHT_DP) to widgetViews(
            app,
            R.layout.widget_player,
            station,
            artwork,
            playbackDisplay?.title,
            playbackDisplay?.artist,
            playback,
        ),
        widgetSize(WIDGET_NARROW_WIDTH_DP, WIDGET_PANE_HEIGHT_DP) to widgetViews(
            app,
            R.layout.widget_player_expanded,
            station,
            artwork,
            playbackDisplay?.title,
            playbackDisplay?.artist,
            playback,
        ),
        widgetSize(WIDGET_WIDE_WIDTH_DP, WIDGET_PANE_HEIGHT_DP) to widgetViews(
            app,
            R.layout.widget_player_expanded,
            station,
            artwork,
            playbackDisplay?.title,
            playbackDisplay?.artist,
            playback,
        ),
    )
    if (!shouldPublish()) return
    AppWidgetManager.getInstance(app).updateWidgetLayouts(app, layouts)
}

private fun widgetViews(
    app: AerialApp,
    layoutId: Int,
    station: Station?,
    artwork: Bitmap?,
    displayTitle: CharSequence?,
    displaySubtitle: CharSequence?,
    playback: WidgetPlaybackState,
    hasArtwork: Boolean = true,
    hasText: Boolean = true,
): RemoteViews = RemoteViews(app.packageName, layoutId).apply {
    if (hasText) {
        setTextViewText(R.id.widget_station_name, displayTitle ?: app.getString(R.string.widget_empty))
        setBoolean(R.id.widget_station_name, "setSelected", true)
        setViewVisibility(
            R.id.widget_live_radio,
            if (station == null) android.view.View.GONE else android.view.View.VISIBLE,
        )
        setTextViewText(R.id.widget_live_radio, displaySubtitle)
        setBoolean(R.id.widget_live_radio, "setSelected", true)
    }
    if (hasArtwork) {
        if (artwork == null) {
            setImageViewResource(R.id.widget_station_artwork, R.mipmap.ic_launcher)
        } else {
            setImageViewBitmap(R.id.widget_station_artwork, artwork)
        }
        setContentDescription(R.id.widget_station_artwork, station?.name)
    }
    setImageViewResource(
        R.id.widget_play_pause,
        if (playback.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
    )
    setContentDescription(
        R.id.widget_play_pause,
        app.getString(if (playback.isPlaying) R.string.widget_pause else R.string.widget_play),
    )
    setViewVisibility(
        R.id.widget_previous,
        if (playback.canSkipPrevious) android.view.View.VISIBLE else android.view.View.INVISIBLE,
    )
    setViewVisibility(
        R.id.widget_next,
        if (playback.canSkipNext) android.view.View.VISIBLE else android.view.View.INVISIBLE,
    )
    setInt(
        R.id.widget_play_pause,
        "setBackgroundResource",
        if (playback.isPlaying) {
            R.drawable.widget_control_primary_playing
        } else {
            R.drawable.widget_control_primary_paused
        },
    )
    setOnClickPendingIntent(
        R.id.widget_previous,
        widgetPendingIntent(app, ACTION_WIDGET_PREVIOUS, 1),
    )
    setOnClickPendingIntent(
        R.id.widget_play_pause,
        widgetPendingIntent(app, ACTION_WIDGET_TOGGLE, 2),
    )
    setOnClickPendingIntent(
        R.id.widget_next,
        widgetPendingIntent(app, ACTION_WIDGET_NEXT, 3),
    )
    setOnClickPendingIntent(
        android.R.id.background,
        widgetOpenAppPendingIntent(app),
    )
}

private fun AppWidgetManager.updateWidgetLayouts(
    context: Context,
    layouts: Map<SizeF, RemoteViews>,
) {
    val component = ComponentName(context, AerialWidgetReceiver::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        updateAppWidget(component, RemoteViews(layouts))
        return
    }
    getAppWidgetIds(component).forEach { id ->
        val options = getAppWidgetOptions(id)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val size = widgetLayoutSize(width, height)
        updateAppWidget(id, layouts.getValue(widgetSize(size.width, size.height)))
    }
}

internal data class WidgetLayoutSize(val width: Int, val height: Int)

internal fun widgetLayoutSize(width: Int, height: Int): WidgetLayoutSize = when {
    height >= WIDGET_PANE_HEIGHT_DP && width >= WIDGET_WIDE_WIDTH_DP ->
        WidgetLayoutSize(WIDGET_WIDE_WIDTH_DP, WIDGET_PANE_HEIGHT_DP)
    height >= WIDGET_PANE_HEIGHT_DP ->
        WidgetLayoutSize(WIDGET_NARROW_WIDTH_DP, WIDGET_PANE_HEIGHT_DP)
    height >= WIDGET_TALL_HEIGHT_DP && width >= WIDGET_WIDE_WIDTH_DP ->
        WidgetLayoutSize(WIDGET_WIDE_WIDTH_DP, WIDGET_TALL_HEIGHT_DP)
    height >= WIDGET_TALL_HEIGHT_DP -> WidgetLayoutSize(WIDGET_NARROW_WIDTH_DP, WIDGET_TALL_HEIGHT_DP)
    height >= WIDGET_WAFER_HEIGHT_DP && width >= WIDGET_WIDE_WIDTH_DP ->
        WidgetLayoutSize(WIDGET_WIDE_WIDTH_DP, WIDGET_WAFER_HEIGHT_DP)
    height >= WIDGET_WAFER_HEIGHT_DP ->
        WidgetLayoutSize(WIDGET_NARROW_WIDTH_DP, WIDGET_WAFER_HEIGHT_DP)
    width >= WIDGET_WIDE_WIDTH_DP -> WidgetLayoutSize(WIDGET_WIDE_WIDTH_DP, WIDGET_STICK_HEIGHT_DP)
    else -> WidgetLayoutSize(WIDGET_NARROW_WIDTH_DP, WIDGET_STICK_HEIGHT_DP)
}

private fun widgetSize(width: Int, height: Int) = SizeF(width.toFloat(), height.toFloat())

private fun isMeaningfulWidgetMetadata(value: String): Boolean = value.any(Char::isLetterOrDigit)

private fun widgetPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent =
    PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, AerialWidgetActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

private fun widgetOpenAppPendingIntent(context: Context): PendingIntent =
    PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

private suspend fun selectedStation(
    app: AerialApp,
    favorites: List<Station>,
    mediaId: String?,
): Station? = favorites.firstOrNull { it.id.toString() == mediaId }
    ?: PlaybackSnapshotStore(app.settingsDataStore).read()?.station
    ?: favorites.firstOrNull()

private suspend fun stationArtwork(context: Context, station: Station): Bitmap? =
    withTimeoutOrNull(ARTWORK_TIMEOUT_MS) {
        runCatching {
            val metadata = station.toSystemPlayableMediaItem(context).mediaMetadata
            val artwork = metadata.artworkUri ?: metadata.artworkData ?: return@runCatching null
            val request = ImageRequest.Builder(context)
                .data(artwork)
                .size(MAX_ARTWORK_SIZE_PX)
                .build()
            val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult
            val bitmap = result?.image?.toTransparentBitmap()
            bitmap?.scaledForWidget()?.maskedForWidget()
        }.getOrNull()
    }

private fun Bitmap.scaledForWidget(): Bitmap {
    val largestSide = maxOf(width, height)
    if (largestSide <= MAX_ARTWORK_SIZE_PX) return this
    val scale = MAX_ARTWORK_SIZE_PX.toFloat() / largestSide
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
        true,
    )
}

private fun Bitmap.maskedForWidget(): Bitmap {
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = BitmapShader(this@maskedForWidget, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    val bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
    if (hasCircularArtwork()) {
        canvas.drawOval(bounds, paint)
    } else {
        val radius = minOf(width, height) * ARTWORK_CORNER_FRACTION
        canvas.drawRoundRect(bounds, radius, radius, paint)
    }
    return result
}

private suspend fun withController(
    context: Context,
    block: suspend (MediaController) -> Unit,
) = withContext(Dispatchers.Main.immediate) {
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

private suspend fun favorites(context: Context): List<Station> {
    val app = context.applicationContext as AerialApp
    return stationsForWidget(app.repository.getAll().first())
}

private fun setStationQueue(
    context: Context,
    controller: MediaController,
    stations: List<Station>,
    index: Int,
) {
    controller.setMediaItems(
        stations.map { it.toPlayableMediaItem(context.applicationContext) },
        index,
        androidx.media3.common.C.TIME_UNSET,
    )
    controller.prepare()
    controller.play()
}

private suspend fun handleWidgetPlaybackAction(context: Context, action: String?) {
    withController(context) { controller ->
        when (action) {
            ACTION_WIDGET_PREVIOUS -> if (controller.hasPreviousMediaItem()) {
                controller.seekToPreviousMediaItem()
                controller.play()
            }
            ACTION_WIDGET_NEXT -> if (controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
                controller.play()
            }
            ACTION_WIDGET_TOGGLE -> when {
                controller.playWhenReady -> controller.pause()
                controller.currentMediaItem != null -> controller.play()
                else -> restoreWidgetQueue(context, controller)
            }
        }
    }
}

private suspend fun restoreWidgetQueue(context: Context, controller: MediaController) {
    val app = context.applicationContext as AerialApp
    val snapshot = PlaybackSnapshotStore(app.settingsDataStore).read()
    val stations = snapshot?.queue?.takeIf { it.isNotEmpty() }
        ?: snapshot?.station?.let(::listOf)
        ?: favorites(context).firstOrNull()?.let(::listOf)
        ?: return
    val selected = snapshot?.station?.let { current ->
        stations.indexOfFirst { it.matches(current) }.takeIf { it >= 0 }
    } ?: 0
    setStationQueue(context, controller, stations, selected)
}

class AerialWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        (context.applicationContext as AerialApp).applicationScope.launch {
            try {
                handleWidgetPlaybackAction(context.applicationContext, intent.action)
            } catch (error: Throwable) {
                Log.e(TAG, "Playback action failed: ${intent.action}", error)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class AerialWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        requestAerialWidgetUpdate(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?,
    ) {
        requestAerialWidgetUpdate(context)
    }
}

private const val TAG = "AerialWidget"
private const val ARTWORK_TIMEOUT_MS = 3_000L
private const val MAX_ARTWORK_SIZE_PX = 256
private const val ARTWORK_CORNER_FRACTION = 0.2f
private const val WIDGET_NARROW_WIDTH_DP = 180
private const val WIDGET_WIDE_WIDTH_DP = 304
private const val WIDGET_STICK_HEIGHT_DP = 48
private const val WIDGET_WAFER_HEIGHT_DP = 80
private const val WIDGET_TALL_HEIGHT_DP = 152
private const val WIDGET_PANE_HEIGHT_DP = 272
