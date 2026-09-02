package com.shapeshed.aerial.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
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
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
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

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
)
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
    val trackTitle = currentTrackTitle
    val trackArtist = currentTrackArtist?.takeIf { it.isNotBlank() && it != station.name }
    val activeArtworkModel = station.ownLogoModel()
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
    val playbackActionDescription = stringResource(
        when {
            isBuffering -> R.string.buffering
            isPlaying -> R.string.pause
            else -> R.string.play
        },
    )
    // When the timer clears (it expired, or was cancelled), close the picker too. Keyed on the
    // active->inactive transition so a picker opened with no timer running stays open.
    LaunchedEffect(sleepTimer == null) { if (sleepTimer == null) showSleepTimer = false }

    Scaffold(
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
                .consumeWindowInsets(padding)
                .padding(horizontal = 16.dp)
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
            // Short panes reserve more room for scalable text and fixed controls. On a normal
            // portrait pane the hero grows to nearly the available width, matching the visual
            // hierarchy of established audio players without compromising 200% text layouts.
            val artworkFraction = when {
                maxHeight < 520.dp -> 0.28f
                maxHeight < 700.dp -> 0.38f
                else -> 0.52f
            }
            // On sufficiently tall portrait screens, align the artwork with the same content
            // edges as the metadata and control row. Keep the height cap on short and landscape
            // panes so the hero leaves room for metadata and fixed controls.
            val artworkSize = if (maxWidth <= maxHeight && maxHeight >= 700.dp) {
                maxWidth
            } else {
                (maxHeight * artworkFraction).coerceAtMost(maxWidth)
            }
            val contentWidth = maxWidth
            // Match the metadata's maximum edges to the hero on regular-height panes. When the
            // hero is deliberately reduced on a short pane, keep the readable content width so
            // large text does not get forced into the artwork's much narrower measure.
            val trackSurfaceMaxWidth = if (maxHeight >= 700.dp) artworkSize else contentWidth

            Column(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(artworkSize)
                        .semantics { traversalIndex = 1f }
                        .testTag("now_playing_artwork"),
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
                            StationArtworkSurface(
                                artworkModel = if (pageStation.matches(station)) {
                                    activeArtworkModel
                                } else {
                                    pageStation.ownLogoModel()
                                },
                            )
                        }
                    } else {
                        StationArtworkSurface(artworkModel = activeArtworkModel)
                    }
                }

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 12.dp)
                        .semantics { traversalIndex = 2f },
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (showStreamBitrate) {
                        AnimatedContent(
                            targetState = bitrateLabel,
                            contentKey = { it != null },
                            transitionSpec = {
                                (fadeIn() togetherWith fadeOut())
                                    .using(SizeTransform(clip = false))
                            },
                            label = "streamBitrate",
                        ) { label ->
                            if (label != null) StreamBitratePill(text = label)
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        FilledTonalIconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(48.dp)
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
                            modifier = Modifier
                                .size(72.dp)
                                .semantics {
                                    traversalIndex = 6f
                                    contentDescription = playbackActionDescription
                                },
                        ) {
                            if (isBuffering) {
                                val indicatorColor = LocalContentColor.current
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = indicatorColor,
                                    trackColor = indicatorColor.copy(alpha = 0.3f),
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
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
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { traversalIndex = 8f },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.share_station),
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .nestedScroll(dismissNestedScrollConnection)
                        .verticalScroll(contentScrollState)
                        .padding(top = 12.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (showTrackBlock) {
                        Surface(
                            modifier = Modifier
                                .widthIn(max = trackSurfaceMaxWidth)
                                .semantics { traversalIndex = 9f }
                                .testTag("now_playing_track_surface"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Row(
                                modifier = Modifier
                                    .heightIn(min = 64.dp)
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                IconButton(
                                    onClick = {
                                        val copyText = buildString {
                                            if (!trackArtist.isNullOrBlank()) append(trackArtist)
                                            if (!trackArtist.isNullOrBlank() && !trackTitle.isNullOrBlank()) append(" — ")
                                            if (!trackTitle.isNullOrBlank()) append(trackTitle)
                                        }
                                        clipboard.setPrimaryClip(ClipData.newPlainText("track", copyText))
                                    },
                                    modifier = Modifier.semantics { traversalIndex = 10f },
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.copy_track_info))
                                }
                                Column(
                                    modifier = Modifier
                                        .widthIn(max = (trackSurfaceMaxWidth - 56.dp).coerceAtLeast(0.dp))
                                        .padding(end = 12.dp),
                                    horizontalAlignment = Alignment.Start,
                                ) {
                                    Text(
                                        text = trackTitle.orEmpty(),
                                        style = MaterialTheme.typography.titleMediumEmphasized,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.testTag("now_playing_track_title"),
                                    )
                                    if (!trackArtist.isNullOrBlank() && trackArtist != trackTitle) {
                                        Text(
                                            text = trackArtist,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.testTag("now_playing_track_artist"),
                                        )
                                    }
                                }
                            }
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
    artworkModel: Any?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        // Artwork sits directly on the now-playing screen surface. Circular artwork reveals
        // that same surface through its transparent regions.
        StationLogoSurface(
            logoModel = artworkModel,
            size = maxWidth,
            fallbackBackground = MaterialTheme.colorScheme.background,
        ) {
            Icon(
                imageVector = Icons.Rounded.Radio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(maxWidth * 0.28f),
            )
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
        modifier = modifier.testTag("now_playing_bitrate"),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}
