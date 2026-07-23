package com.shapeshed.aerial.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.NowPlayingInfo
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
    nowPlayingInfo: NowPlayingInfo? = null,
    currentTrackTitle: String?,
    currentTrackArtist: String? = null,
    currentTrackArtworkData: ByteArray? = null,
    currentTrackArtworkUrl: String? = null,
    currentBitrateKbps: Int? = null,
    showStreamBitrate: Boolean = false,
    sleepTimer: SleepTimerState? = null,
    swipeStations: List<Station> = emptyList(),
    onPlayStation: (Station) -> Unit = {},
    onToggle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetSleepTimer: (Long) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val shareStationLabel = stringResource(R.string.share_station)
    val dismissThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissScope = rememberCoroutineScope()
    // Horizontal paging steps through swipeStations (the favourites in their selected sort
    // order). A non-favourite station isn't in the list, so the artwork stays static for it.
    val swipeIndex = remember(swipeStations, station) {
        swipeStations.indexOfFirst { it.matches(station) }
    }
    var showTrackDetail by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    val artworkShape = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 6.dp,
        bottomEnd = 28.dp,
    )
    val programmeTitle = nowPlayingInfo?.programmeTitle ?: station.name
    val programmeSubtitle = nowPlayingInfo?.programmeSubtitle
    val track = nowPlayingInfo?.track
    val trackTitle = when {
        track != null -> track.title
        nowPlayingInfo != null -> null  // enricher active but no track data — don't fall back to ICY
        else -> currentTrackTitle
    }
    val trackArtist = track?.artist?.takeIf { it.isNotBlank() }
        // No enricher: use the ICY artist (ignoring one that's just the station name).
        ?: currentTrackArtist?.takeIf { nowPlayingInfo == null && it.isNotBlank() && it != station.name }
    val mainArtworkModel = when {
        nowPlayingInfo?.artworkData != null -> nowPlayingInfo.artworkData
        !nowPlayingInfo?.artworkUrl.isNullOrBlank() -> nowPlayingInfo.artworkUrl
        // Programme-context enrichment (BBC-style): if artwork fetch failed, fall back to the
        // artwork written to the media item by applyNowPlayingInfo (programme/track bytes or
        // the original station logo). MusicBrainz doesn't set programmeTitle, so it stays
        // blocked here to avoid showing album art as the main station image.
        nowPlayingInfo?.programmeTitle != null && track == null && currentTrackArtworkData != null -> currentTrackArtworkData
        nowPlayingInfo?.programmeTitle != null && track == null && !currentTrackArtworkUrl.isNullOrBlank() -> currentTrackArtworkUrl
        nowPlayingInfo == null && currentTrackArtworkData != null -> currentTrackArtworkData
        nowPlayingInfo == null && !currentTrackArtworkUrl.isNullOrBlank() -> currentTrackArtworkUrl
        else -> null
    }
    // The main image is distinct programme/context artwork (not the station's own logo) only
    // when the enricher supplied its own artwork. Otherwise the main image already is the
    // station logo, so the small logo badge would just duplicate it.
    val mainArtworkIsDistinct = nowPlayingInfo?.artworkData != null || !nowPlayingInfo?.artworkUrl.isNullOrBlank()
    // When there's no distinct enrichment artwork, mainArtworkModel is just whatever play()
    // wrote into the MediaItem for system media surfaces: the station's own logo if it has
    // one, or the app icon as a last resort if it doesn't. That value also arrives
    // asynchronously (play() clears it, and it only repopulates once the media session's
    // metadata callback fires again), so using it here would both flash through the small
    // circular avatar for a frame after every swipe and, for a logo-less station, "upgrade"
    // to showing the app's launcher icon as if it were the station's own artwork. Resolving
    // the station's own logo directly — synchronously, no gap — avoids both: it's the exact
    // image the pager already showed for this page before it became active, and it's simply
    // absent (falling back to the avatar, not the app icon) for a station with no logo.
    val activeArtworkModel = if (mainArtworkIsDistinct) mainArtworkModel else station.ownLogoModel()
    val trackArtworkModel = when {
        track?.artworkData != null -> track.artworkData
        track?.artworkUrl?.isNotBlank() == true -> track.artworkUrl
        currentTrackArtworkData != null -> currentTrackArtworkData
        !currentTrackArtworkUrl.isNullOrBlank() -> currentTrackArtworkUrl
        else -> null
    }
    var mainArtworkFailed by remember(activeArtworkModel) { mutableStateOf(false) }
    var trackArtworkFailed by remember(trackArtworkModel) { mutableStateOf(false) }
    var trackArtworkIsLight by remember(trackArtworkModel) { mutableStateOf(false) }
    // The track flows reset asynchronously when the station changes, so for a frame or two
    // the previous station's song can pair with the new station. Hide the track card from
    // the moment the station changes until that reset has been observed, so stale track
    // info never flashes on the new page. On the pane's first composition the metadata is
    // live for the current station (e.g. opened mid-song from the mini player), so it shows
    // immediately — only an in-pane station change arms the gate.
    val seenStreamUrl = remember { mutableStateOf<String?>(null) }
    var trackResetSeen by remember(station.streamUrl) {
        val stationChanged = seenStreamUrl.value != null && seenStreamUrl.value != station.streamUrl
        seenStreamUrl.value = station.streamUrl
        mutableStateOf(!stationChanged || trackTitle == null)
    }
    LaunchedEffect(trackTitle, station.streamUrl) {
        if (trackTitle == null) trackResetSeen = true
    }
    val showTrackBlock = trackResetSeen && !trackTitle.isNullOrBlank() && trackTitle != programmeTitle
    val bitrateLabel = currentBitrateKbps
        ?.takeIf { showStreamBitrate && it > 0 }
        ?.let { stringResource(R.string.stream_bitrate_format, it) }
    LaunchedEffect(showTrackBlock) { if (!showTrackBlock) showTrackDetail = false }
    // When the timer clears (it expired, or was cancelled), close the picker too. Keyed on the
    // active->inactive transition so a picker opened with no timer running stays open.
    LaunchedEffect(sleepTimer == null) { if (sleepTimer == null) showSleepTimer = false }

    Scaffold(
        modifier = Modifier
            .graphicsLayer { translationY = dragOffsetY }
            .semantics { isTraversalGroup = true }
            .pointerInput(dismissThresholdPx, onDismiss) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        val nextOffset = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                        if (nextOffset > 0f) {
                            change.consume()
                        }
                        dragOffsetY = nextOffset
                    },
                    onDragEnd = {
                        if (dragOffsetY >= dismissThresholdPx) {
                            onDismiss()
                            // Deferred, not immediate: resetting dragOffsetY synchronously here
                            // would zero out this graphicsLayer's translationY on the same frame
                            // the outer AnimatedVisibility's own exit slide starts from *its*
                            // zero baseline, so the pane visibly snapped up before sliding back
                            // down. Waiting until well after the exit animation has finished
                            // avoids that flicker, while still guaranteeing the offset is clean
                            // before a fast reopen — the scenario this reset exists for — could
                            // plausibly reuse this composable's state mid-exit.
                            dismissScope.launch {
                                delay(500)
                                dragOffsetY = 0f
                            }
                        } else {
                            dragOffsetY = 0f
                        }
                    },
                    onDragCancel = {
                        dragOffsetY = 0f
                    },
                )
            },
        topBar = {
            TopAppBar(
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
                .padding(horizontal = 24.dp),
        ) {
            // Cap the artwork to a share of the pane's height rather than letting it fill the
            // full width unconditionally — on a wide landscape/tablet window (the new nav rail
            // layout) that let the square artwork blow up to the pane's full width and dominate
            // the screen.
            val artworkSize = (maxHeight * 0.42f).coerceAtMost(maxWidth)

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(artworkSize)
                        .semantics { traversalIndex = 1f },
                ) {
                    if (swipeIndex != -1 && swipeStations.size > 1) {
                        // Real pager over the frozen favourites order: the neighbour's artwork is a
                        // prepared page that slides in with the finger; settling switches playback.
                        // The rest of the pane stays static.
                        val virtualPageCount = Int.MAX_VALUE
                        val initialPage = remember(swipeStations, swipeIndex) {
                            val midpoint = virtualPageCount / 2
                            midpoint - circularPageIndex(midpoint, swipeStations.size) + swipeIndex
                        }
                        val pagerState = rememberPagerState(initialPage = initialPage) { virtualPageCount }
                        LaunchedEffect(pagerState.settledPage) {
                            val target = swipeStations[circularPageIndex(pagerState.settledPage, swipeStations.size)]
                            if (!target.matches(station)) onPlayStation(target)
                        }
                        // Keep the pager in step when the station changes some other way (e.g. the
                        // media notification or a tap on the favourites grid behind the pane).
                        LaunchedEffect(swipeIndex) {
                            val currentIndex = circularPageIndex(pagerState.currentPage, swipeStations.size)
                            if (currentIndex != swipeIndex && !pagerState.isScrollInProgress) {
                                val forwardDelta = circularPageIndex(swipeIndex - currentIndex, swipeStations.size)
                                val backwardDelta = forwardDelta - swipeStations.size
                                val delta = if (abs(backwardDelta) < forwardDelta) backwardDelta else forwardDelta
                                pagerState.animateScrollToPage(pagerState.currentPage + delta)
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
                                // Own logo, full-bleed — not the small circular avatar, and not
                                // sharing mainArtworkFailed with the active page (a neighbour's
                                // logo failing to load must not blank out the real artwork above).
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
                    if (mainArtworkModel != null && !mainArtworkFailed && mainArtworkIsDistinct) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 6.dp,
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 12.dp, end = 12.dp)
                                .size(56.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                StationAvatar(
                                    station = station,
                                    isActive = false,
                                    size = 44.dp,
                                )
                            }
                        }
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
                // Every block below reserves its space even when empty (blank text keeps its
                // line height, the track card keeps a fixed slot) so the centred column doesn't
                // jump vertically when swiping between stations with different metadata.
                Spacer(Modifier.height(28.dp))
                Text(
                    text = if (nowPlayingInfo != null) station.name else "",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = programmeTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { traversalIndex = 2f },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = programmeSubtitle?.takeIf { it != programmeTitle } ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Only enriched stations (nowPlayingInfo != null) can ever populate this
                    // line, and only they need the full 2-line reservation to avoid a jump as
                    // that data arrives/changes. A station with no enricher at all (most ICY
                    // stations) never has a subtitle, so reserving a stable 2 blank lines for
                    // it here was permanently wasted space — most visibly on small screens,
                    // where it pushed the now-playing track card off the bottom of the pane.
                    minLines = if (nowPlayingInfo != null) 2 else 1,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { traversalIndex = 3f },
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                tint = if (station.isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            )
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
                                .semantics { traversalIndex = 5f },
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
                                .semantics { traversalIndex = 6f },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.share_station),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                // Fixed-height slot whether or not track info exists — the card fades in and
                // out inside it, so the layout above never shifts. Fully qualified because the
                // enclosing ColumnScope's AnimatedVisibility extension shadows the top-level one.
                Box(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showTrackBlock,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTrackDetail = true },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(52.dp),
                            ) {
                                if (trackArtworkModel != null && !trackArtworkFailed) {
                                    // Same adaptive plate as the other artwork surfaces,
                                    // visible only through transparent artwork.
                                    AsyncImage(
                                        model = trackArtworkModel,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        onError = { trackArtworkFailed = true },
                                        onSuccess = { state ->
                                            trackArtworkIsLight = state.result.image.isPredominantlyLight()
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(MaterialTheme.shapes.small)
                                            .background(stationLogoPlateColor(trackArtworkIsLight)),
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        tonalElevation = 2.dp,
                                        modifier = Modifier.size(44.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(
                                                imageVector = Icons.Rounded.Radio,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = trackArtist ?: trackTitle.orEmpty(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!trackArtist.isNullOrBlank() && !trackTitle.isNullOrBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = trackTitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(R.string.copy_track_info),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
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

    if (showTrackDetail) {
        val sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val copyText = buildString {
            if (!trackArtist.isNullOrBlank()) append(trackArtist)
            if (!trackArtist.isNullOrBlank() && !trackTitle.isNullOrBlank()) append(" — ")
            if (!trackTitle.isNullOrBlank()) append(trackTitle)
        }
        // Staggered entry states
        var artworkReady by remember { mutableStateOf(false) }
        var artistReady by remember { mutableStateOf(false) }
        var titleReady by remember { mutableStateOf(false) }
        var actionsReady by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            artworkReady = true
            delay(80)
            artistReady = true
            delay(60)
            titleReady = true
            delay(50)
            actionsReady = true
        }

        val artworkScale by animateFloatAsState(
            targetValue = if (artworkReady) 1f else 0.82f,
            label = "artworkScale",
        )
        val artworkAlpha by animateFloatAsState(
            targetValue = if (artworkReady) 1f else 0f,
            label = "artworkAlpha",
        )

        ModalBottomSheet(
            onDismissRequest = { showTrackDetail = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    // Plate behind rendered artwork, matching the main artwork surface.
                    color = if (trackArtworkModel != null && !trackArtworkFailed) {
                        stationLogoPlateColor(trackArtworkIsLight)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .graphicsLayer {
                            scaleX = artworkScale
                            scaleY = artworkScale
                            alpha = artworkAlpha
                        },
                ) {
                    if (trackArtworkModel != null && !trackArtworkFailed) {
                        AsyncImage(
                            model = trackArtworkModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            onError = { trackArtworkFailed = true },
                            onSuccess = { state ->
                                trackArtworkIsLight = state.result.image.isPredominantlyLight()
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            StationAvatar(
                                station = station,
                                isActive = false,
                                size = 96.dp,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        AnimatedVisibility(
                            visible = artistReady,
                            enter = fadeIn() + slideInVertically { it / 3 },
                        ) {
                            Text(
                                text = trackArtist ?: trackTitle.orEmpty(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!trackArtist.isNullOrBlank() && !trackTitle.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            AnimatedVisibility(
                                visible = titleReady,
                                enter = fadeIn() + slideInVertically { it / 3 },
                            ) {
                                Text(
                                    text = trackTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    AnimatedVisibility(
                        visible = actionsReady,
                        enter = fadeIn() + scaleIn(initialScale = 0.6f),
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                clipboard.setPrimaryClip(ClipData.newPlainText("track", copyText))
                            },
                            shapes = IconButtonShapes(
                                IconButtonDefaults.smallRoundShape,
                                IconButtonDefaults.smallPressedShape,
                            ),
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.copy_track_info))
                        }
                    }
                }
            }
        }
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
        tonalElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (artworkModel != null) {
                val context = LocalContext.current
                // Crossfade rather than pop: activeArtworkModel legitimately changes after a
                // swipe settles (station logo shown first, upgraded to real enrichment artwork
                // once the metadata pipeline catches up), and a hard cut reads as a flash.
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
                    onSuccess = { state -> artworkIsLight = state.result.image.isPredominantlyLight() },
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
        tonalElevation = 1.dp,
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
