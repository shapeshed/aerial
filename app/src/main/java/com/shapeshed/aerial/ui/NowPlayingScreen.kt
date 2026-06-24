package com.shapeshed.aerial.ui

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.shapeshed.aerial.data.NowPlayingInfo
import com.shapeshed.aerial.data.Station

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NowPlayingScreen(
    station: Station,
    isPlaying: Boolean,
    isBuffering: Boolean,
    bitrateKbps: Int?,
    showBitrate: Boolean = false,
    nowPlayingInfo: NowPlayingInfo? = null,
    currentTrackTitle: String?,
    currentTrackArtworkData: ByteArray? = null,
    currentTrackArtworkUrl: String? = null,
    monochromeLogos: Boolean = false,
    onToggle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val dismissThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
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
    val trackArtist = track?.artist
    val mainArtworkModel = when {
        nowPlayingInfo?.artworkData != null -> nowPlayingInfo.artworkData
        !nowPlayingInfo?.artworkUrl.isNullOrBlank() -> nowPlayingInfo.artworkUrl
        // Programme-context enrichment (BBC-style): if artwork fetch failed, fall back to the
        // artwork written to the media item by applyNowPlayingInfo (programme/track bytes or
        // the original station logo). MusicBrainz doesn't set programmeTitle, so it stays
        // blocked here to avoid showing album art as the main station image.
        nowPlayingInfo?.programmeTitle != null && currentTrackArtworkData != null -> currentTrackArtworkData
        nowPlayingInfo?.programmeTitle != null && !currentTrackArtworkUrl.isNullOrBlank() -> currentTrackArtworkUrl
        nowPlayingInfo == null && currentTrackArtworkData != null -> currentTrackArtworkData
        nowPlayingInfo == null && !currentTrackArtworkUrl.isNullOrBlank() -> currentTrackArtworkUrl
        else -> null
    }
    val trackArtworkModel = when {
        track?.artworkData != null -> track.artworkData
        !track?.artworkUrl.isNullOrBlank() -> track!!.artworkUrl
        currentTrackArtworkData != null -> currentTrackArtworkData
        !currentTrackArtworkUrl.isNullOrBlank() -> currentTrackArtworkUrl
        else -> null
    }
    var mainArtworkFailed by remember(mainArtworkModel) { mutableStateOf(false) }
    var trackArtworkFailed by remember(trackArtworkModel) { mutableStateOf(false) }
    val showTrackBlock = !trackTitle.isNullOrBlank() && trackTitle != programmeTitle
    val bitrateText = when {
        bitrateKbps == null -> null
        showBitrate -> "$bitrateKbps kbps"
        bitrateKbps >= 128 -> "HD"
        else -> null
    }

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
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close player")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { traversalIndex = 1f },
            ) {
                Surface(
                    shape = artworkShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (mainArtworkModel != null && !mainArtworkFailed) {
                            AsyncImage(
                                model = mainArtworkModel,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                onError = { mainArtworkFailed = true },
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            StationAvatar(
                                station = station,
                                isActive = true,
                                size = 200.dp,
                                monochrome = monochromeLogos,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
                if (mainArtworkModel != null && !mainArtworkFailed) {
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
                                monochrome = monochromeLogos,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            if (nowPlayingInfo != null) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = programmeTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { traversalIndex = 2f },
            )
            if (programmeSubtitle != null && programmeSubtitle != programmeTitle) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = programmeSubtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { traversalIndex = 3f },
                )
            }
            if (bitrateText != null) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = bitrateText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(44.dp))
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
                            contentDescription = if (station.isFavorite) "Remove from favorites" else "Add to favorites",
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
                                    contentDescription = if (playing) "Pause" else "Play",
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
                                Intent.createChooser(sendIntent, "Share station"),
                            )
                        },
                        shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                        modifier = Modifier
                            .size(56.dp)
                            .semantics { traversalIndex = 6f },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share station",
                        )
                    }
                }
            }
            if (showTrackBlock) {
                Spacer(Modifier.height(20.dp))
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
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
                                AsyncImage(
                                    model = trackArtworkModel,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    onError = { trackArtworkFailed = true },
                                    modifier = Modifier.fillMaxSize(),
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
                    }
                }
            }
        }
    }
}
