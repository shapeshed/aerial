package com.shapeshed.aerial.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.SleepTimerState
import com.shapeshed.aerial.data.Station
import java.io.File
import kotlin.math.abs

private fun circularPageIndex(page: Int, size: Int): Int = ((page % size) + size) % size

// A station's own saved logo, resolved the same way StationAvatar does. Used as full-bleed
// artwork for swipe-pager pages that aren't the actively playing station: those have no live
// "now playing" metadata (that pipeline only feeds the active MediaController), but the
// station's own logo is still real artwork — showing it full-bleed keeps every page visually
// consistent, instead of falling back to the small circular avatar meant for list rows.
private fun Station.ownLogoModel(): Any? = when {
    logoPath.startsWith("http") -> logoPath
    logoPath.isNotEmpty() -> File(logoPath)
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NowPlayingScreen(
    station: Station,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentTrackTitle: String?,
    currentTrackArtist: String?,
    currentBitrateKbps: Int?,
    showStreamBitrate: Boolean,
    sleepTimer: SleepTimerState?,
    swipeStations: List<Station>,
    onPlayStation: (Station) -> Unit,
    onPreviousStation: () -> Unit,
    onNextStation: () -> Unit,
    onToggle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetSleepTimer: (Long) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shareStationLabel = stringResource(R.string.share_station)
    val dismissThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissScope = rememberCoroutineScope()
    // Horizontal paging steps through swipeStations (the favourites in their selected sort
    // order). A non-favourite station isn't in the list, so the artwork stays static for it.
    val swipeIndex = remember(swipeStations, station) {
        swipeStations.indexOfFirst { it.matches(station) }
    }
    val hasStationNavigation = swipeIndex >= 0 && swipeStations.size > 1
    var showSleepTimer by remember { mutableStateOf(false) }
    val artworkShape = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 6.dp,
        bottomEnd = 28.dp,
    )
    val trackTitle = currentTrackTitle
    val trackArtist = currentTrackArtist?.takeIf { it.isNotBlank() && it != station.name }
    val activeArtworkModel = station.ownLogoModel()
    var mainArtworkFailed by remember(activeArtworkModel) { mutableStateOf(false) }
    val contentScrollState = rememberScrollState()
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val dismissNestedScrollConnection = remember(contentScrollState, onDismiss, dismissThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y > 0f && contentScrollState.value == 0) {
                    dragOffsetY = (dragOffsetY + available.y).coerceAtLeast(0f)
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dragOffsetY >= dismissThresholdPx) {
                    onDismiss()
                    dismissScope.launch {
                        delay(500)
                        dragOffsetY = 0f
                    }
                    return available
                }
                if (dragOffsetY > 0f) dragOffsetY = 0f
                return Velocity.Zero
            }
        }
    }
    // Station identity and stream metadata are published in one PlaybackUiState snapshot, so
    // metadata can be rendered directly without composition-time reset bookkeeping.
    val showTrackBlock = !trackTitle.isNullOrBlank() && trackTitle != station.name
    val bitrateLabel = currentBitrateKbps
        ?.takeIf { showStreamBitrate && it > 0 }
        ?.let { stringResource(R.string.stream_bitrate_format, it) }
    // When the timer clears (it expired, or was cancelled), close the picker too. Keyed on the
    // active->inactive transition so a picker opened with no timer running stays open.
    LaunchedEffect(sleepTimer == null) { if (sleepTimer == null) showSleepTimer = false }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .graphicsLayer { translationY = dragOffsetY }
            .semantics { isTraversalGroup = true },
        topBar = {
            TopAppBar(
                modifier = Modifier.pointerInput(dismissThresholdPx, onDismiss) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            if (dragAmount > 0f) {
                                dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            if (dragOffsetY >= dismissThresholdPx) {
                                onDismiss()
                                dismissScope.launch {
                                    delay(500)
                                    dragOffsetY = 0f
                                }
                            } else {
                                dragOffsetY = 0f
                            }
                        },
                        onDragCancel = { dragOffsetY = 0f },
                    )
                },
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                        modifier = Modifier.semantics { traversalIndex = 0f },
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.close_player))
                    }
                },
                actions = {
                    SleepTimerAction(active = sleepTimer, onClick = { showSleepTimer = true })
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .pointerInput(dismissThresholdPx, onDismiss, contentScrollState) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        var dragging = false
                        var directionLocked = false
                        var totalX = 0f
                        var totalY = 0f
                        while (true) {
                            val change = awaitPointerEvent(PointerEventPass.Initial)
                                .changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val positionChange = change.positionChange()
                            totalX += positionChange.x
                            totalY += positionChange.y
                            if (!directionLocked && (totalX * totalX + totalY * totalY) >= touchSlop * touchSlop) {
                                if (abs(totalX) > abs(totalY) || totalY <= 0f) break
                                directionLocked = true
                            }
                            val dragAmount = positionChange.y
                            if (directionLocked && dragAmount > 0f && (dragging || contentScrollState.value == 0)) {
                                dragging = true
                                dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                change.consume()
                            } else if (directionLocked && dragging) {
                                dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                change.consume()
                            }
                        }
                        if (dragging && dragOffsetY >= dismissThresholdPx) {
                            onDismiss()
                            dismissScope.launch {
                                delay(500)
                                dragOffsetY = 0f
                            }
                        } else if (dragging) {
                            dragOffsetY = 0f
                        }
                    }
                },
        ) {
            // Cap the artwork to a share of the pane's height rather than letting it fill the
            // full width unconditionally — on a wide landscape/tablet window (the new nav rail
            // layout) that let the square artwork blow up to the pane's full width and dominate
            // the screen.
            val artworkSize = (maxHeight * 0.42f).coerceAtMost(maxWidth)

            Column(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(artworkSize)
                        .testTag("now_playing_artwork")
                        .semantics { traversalIndex = 1f },
                ) {
                    if (swipeIndex != -1 && swipeStations.size > 1) {
                        // Keep the artwork pager outside the metadata scroller so station
                        // navigation remains available without moving the visual anchor.
                        val virtualPageCount = Int.MAX_VALUE
                        val initialPage = remember(swipeStations, swipeIndex) {
                            val midpoint = virtualPageCount / 2
                            midpoint - circularPageIndex(midpoint, swipeStations.size) + swipeIndex
                        }
                        val pagerState = rememberPagerState(initialPage = initialPage) { virtualPageCount }
                        var isSyncingToStation by remember { mutableStateOf(false) }
                        LaunchedEffect(pagerState.settledPage) {
                            if (isSyncingToStation) return@LaunchedEffect
                            val target = swipeStations[circularPageIndex(pagerState.settledPage, swipeStations.size)]
                            if (!target.matches(station)) onPlayStation(target)
                        }
                        LaunchedEffect(swipeIndex) {
                            val currentIndex = circularPageIndex(pagerState.currentPage, swipeStations.size)
                            if (currentIndex != swipeIndex && !pagerState.isScrollInProgress) {
                                val forwardDelta = circularPageIndex(swipeIndex - currentIndex, swipeStations.size)
                                val backwardDelta = forwardDelta - swipeStations.size
                                val delta = if (abs(backwardDelta) < forwardDelta) backwardDelta else forwardDelta
                                isSyncingToStation = true
                                try {
                                    pagerState.animateScrollToPage(pagerState.currentPage + delta)
                                } finally {
                                    isSyncingToStation = false
                                }
                            }
                        }
                        HorizontalPager(
                            state = pagerState,
                            pageSpacing = 24.dp,
                        ) { page ->
                            val pageStation = swipeStations[circularPageIndex(page, swipeStations.size)]
                            if (pageStation.matches(station)) {
                                StationArtworkSurface(
                                    station = pageStation,
                                    shape = artworkShape,
                                    artworkModel = activeArtworkModel.takeIf { !mainArtworkFailed },
                                    onArtworkError = { mainArtworkFailed = true },
                                )
                            } else {
                                val ownLogoModel = pageStation.ownLogoModel()
                                var ownLogoFailed by remember(ownLogoModel) { mutableStateOf(false) }
                                StationArtworkSurface(
                                    station = pageStation,
                                    shape = artworkShape,
                                    artworkModel = ownLogoModel.takeIf { !ownLogoFailed },
                                    onArtworkError = { ownLogoFailed = true },
                                )
                            }
                        }
                    } else {
                        StationArtworkSurface(
                            station = station,
                            shape = artworkShape,
                            artworkModel = activeArtworkModel.takeIf { !mainArtworkFailed },
                            onArtworkError = { mainArtworkFailed = true },
                        )
                    }
                    if (bitrateLabel != null) {
                        StreamBitratePill(
                            text = bitrateLabel,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 12.dp, bottom = 12.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .nestedScroll(dismissNestedScrollConnection)
                        .verticalScroll(contentScrollState)
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { traversalIndex = 2f },
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    if (showTrackBlock) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start,
                            ) {
                                Text(
                                    text = trackArtist ?: trackTitle.orEmpty(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (!trackArtist.isNullOrBlank() && !trackTitle.isNullOrBlank()) {
                                    Text(
                                        text = trackTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            IconButton(
                                modifier = Modifier.offset(x = 12.dp),
                                onClick = {
                                    val copyText = buildString {
                                        if (!trackArtist.isNullOrBlank()) append(trackArtist)
                                        if (!trackArtist.isNullOrBlank() && !trackTitle.isNullOrBlank()) append(" — ")
                                        if (!trackTitle.isNullOrBlank()) append(trackTitle)
                                    }
                                    clipboard.setPrimaryClip(ClipData.newPlainText("track", copyText))
                                },
                                shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.copy_track_info))
                            }
                        }
                    }
                }
                    Spacer(Modifier.height(24.dp))
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        FilledTonalIconButton(
                            onClick = onToggleFavorite,
                            shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                            modifier = Modifier
                                .size(56.dp)
                                .semantics { traversalIndex = 4f },
                        ) {
                            Icon(
                                imageVector = if (station.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = stringResource(if (station.isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (hasStationNavigation) {
                            FilledTonalIconButton(
                                onClick = onPreviousStation,
                                shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                                modifier = Modifier
                                    .size(56.dp)
                                    .semantics { traversalIndex = 5f },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = stringResource(R.string.previous_station),
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        FilledIconButton(
                            onClick = onToggle,
                            enabled = !isBuffering,
                            modifier = Modifier
                                .size(88.dp)
                                .semantics { traversalIndex = 6f },
                            shapes = IconButtonShapes(IconButtonDefaults.largeRoundShape, IconButtonDefaults.largePressedShape),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            val motionScheme = MaterialTheme.motionScheme
                            AnimatedContent(
                                targetState = isBuffering to isPlaying,
                                transitionSpec = {
                                    (fadeIn(motionScheme.defaultEffectsSpec()) +
                                        scaleIn(motionScheme.defaultSpatialSpec(), initialScale = 0.85f))
                                        .togetherWith(fadeOut(motionScheme.defaultEffectsSpec()))
                                },
                                label = "playPause",
                            ) { (buffering, playing) ->
                                if (buffering) {
                                    CircularWavyProgressIndicator(
                                        modifier = Modifier.size(42.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = stringResource(if (playing) R.string.pause else R.string.play),
                                        modifier = Modifier.size(44.dp),
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (hasStationNavigation) {
                            FilledTonalIconButton(
                                onClick = onNextStation,
                                shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                                modifier = Modifier
                                    .size(56.dp)
                                    .semantics { traversalIndex = 7f },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = stringResource(R.string.next_station),
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, station.name)
                                    putExtra(Intent.EXTRA_TEXT, "${station.name}\n${station.streamUrl}")
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, shareStationLabel),
                                )
                            },
                            shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                            modifier = Modifier
                                .size(56.dp)
                                .semantics { traversalIndex = 8f },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.share_station),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSleepTimer) {
        SleepTimerSheet(
            active = sleepTimer,
            onSet = onSetSleepTimer,
            onCancel = onCancelSleepTimer,
            onDismiss = { showSleepTimer = false },
        )
    }

}

@Composable
private fun StationArtworkSurface(
    station: Station,
    shape: RoundedCornerShape,
    artworkModel: Any?,
    onArtworkError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var artworkIsLight by remember(artworkModel) { mutableStateOf(false) }
    var loadedArtwork by remember(artworkModel) { mutableStateOf<coil3.Image?>(null) }
    LaunchedEffect(artworkModel, loadedArtwork) {
        val image = loadedArtwork ?: return@LaunchedEffect
        artworkIsLight = sharedLogoAppearanceAnalyzer
            .analyze(artworkModel.toString(), image)
            .isLight
    }
    Surface(
        shape = shape,
        // Adaptive plate behind rendered artwork so transparent station logos (and
        // letterboxed images) sit on a consistent, contrasting background; the tonal
        // container shows only for the avatar fallback.
        color = if (artworkModel != null) {
            stationLogoPlateColor(artworkIsLight)
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (artworkModel != null) {
                val context = LocalContext.current
                // Crossfade when the station changes so the artwork transition stays calm.
                val request = remember(context, artworkModel) {
                    ImageRequest.Builder(context)
                        .data(artworkModel)
                        .crossfade(300)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    // Fit (not Crop) so non-square artwork isn't cropped; the
                    // surface colour fills the letterbox space.
                    contentScale = ContentScale.Fit,
                    onError = { onArtworkError() },
                    onSuccess = { state -> loadedArtwork = state.result.image },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // Sized relative to the surface rather than a fixed dp value: every station
                // in the swipe pager besides the one actually playing falls back to this
                // avatar (only the active station has live "now playing" artwork), so a
                // fixed size reads as jarringly smaller than the full-bleed artwork on the
                // active page.
                StationAvatar(
                    station = station,
                    isActive = true,
                    size = maxWidth * 0.56f,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun StreamBitratePill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}
