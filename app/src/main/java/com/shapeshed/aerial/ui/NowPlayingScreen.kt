package com.shapeshed.aerial.ui

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shapeshed.aerial.data.Station

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NowPlayingScreen(
    station: Station,
    isPlaying: Boolean,
    isBuffering: Boolean,
    bitrateKbps: Int?,
    showBitrate: Boolean = false,
    currentTrackTitle: String?,
    monochromeLogos: Boolean = false,
    onToggle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val dismissThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val bitrateText = when {
        bitrateKbps == null -> null
        showBitrate -> "$bitrateKbps kbps"
        bitrateKbps >= 128 -> "HD"
        else -> null
    }

    Scaffold(
        modifier = Modifier
            .graphicsLayer { translationY = dragOffsetY }
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
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Dismiss")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 8.dp,
                modifier = Modifier.size(288.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    StationAvatar(
                        station = station,
                        isActive = true,
                        size = 260.dp,
                        monochrome = monochromeLogos,
                    )
                }
            }
            Spacer(Modifier.height(36.dp))
            Text(
                text = station.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (currentTrackTitle != null && currentTrackTitle != station.name) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = currentTrackTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (bitrateText != null) {
                Spacer(Modifier.height(20.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = bitrateText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
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
                        modifier = Modifier.size(56.dp),
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
                        onClick = { if (!isBuffering) onToggle() },
                        modifier = Modifier.size(88.dp),
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
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share station",
                        )
                    }
                }
            }
        }
    }
}
