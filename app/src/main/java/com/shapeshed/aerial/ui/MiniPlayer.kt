package com.shapeshed.aerial.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.Station

/** Displays compact playback state and delegates every user action to its owner. */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun MiniPlayer(
    station: Station?,
    stationName: String,
    icyInfo: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onHeightChanged: (Int) -> Unit,
    onStop: () -> Unit,
    onTogglePlayback: () -> Unit,
    showNextStation: Boolean,
    onNextStation: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val visibilityState = remember { MutableTransitionState(station != null) }
    visibilityState.targetState = station != null
    androidx.compose.animation.AnimatedVisibility(
        visibleState = visibilityState,
        enter = slideInVertically(
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            initialOffsetY = { it },
        ),
        exit = slideOutVertically(
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            targetOffsetY = { it },
        ),
        modifier = modifier.onSizeChanged { onHeightChanged(it.height) },
    ) {
        station?.let { visibleStation ->
            val dismissState = rememberSwipeToDismissBoxState()
            LaunchedEffect(dismissState.currentValue) {
                if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStop()
                }
            }
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = FloatingToolbarDefaults.ContainerShape,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(
                            contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                Alignment.CenterStart
                            } else {
                                Alignment.CenterEnd
                            },
                            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = stringResource(R.string.stop),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            ) {
                BoxWithConstraints {
                    val active = isPlaying || isBuffering
                    val playPauseCorner by animateDpAsState(
                        targetValue = if (active) 50.dp else 14.dp,
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                        label = "playPauseCorner",
                    )
                    val columnWidth = (maxWidth - if (showNextStation) 196.dp else 140.dp).coerceAtLeast(80.dp)
                    HorizontalFloatingToolbar(
                        expanded = true,
                        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        leadingContent = {
                            StationAvatar(station = visibleStation, isActive = true, size = 52.dp)
                        },
                        trailingContent = {
                            Row(
                                modifier = Modifier.width(if (showNextStation) 108.dp else 52.dp),
                                horizontalArrangement = spacedBy(4.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(playPauseCorner),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(playPauseCorner))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onTogglePlayback()
                                        },
                                ) {
                                val motionScheme = MaterialTheme.motionScheme
                                Box(contentAlignment = Alignment.Center) {
                                    AnimatedContent(
                                        targetState = isBuffering to isPlaying,
                                        transitionSpec = {
                                            (fadeIn(motionScheme.defaultEffectsSpec()) +
                                                scaleIn(motionScheme.defaultSpatialSpec(), initialScale = 0.85f))
                                                .togetherWith(fadeOut(motionScheme.defaultEffectsSpec()))
                                        },
                                        label = "playPauseIcon",
                                    ) { (buffering, playing) ->
                                        if (buffering) {
                                            CircularWavyProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = stringResource(if (playing) R.string.pause else R.string.play),
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(30.dp),
                                            )
                                        }
                                    }
                                }
                                }
                                if (showNextStation) {
                                    Surface(
                                        shape = MaterialTheme.shapes.large,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(MaterialTheme.shapes.large)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onNextStation()
                                            },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.SkipNext,
                                                contentDescription = stringResource(R.string.next_station),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(30.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    ) {
                        Column(
                            modifier = Modifier
                                .width(columnWidth)
                                .clickable(onClick = onExpand)
                                .padding(horizontal = 14.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stationName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.safeMarquee(),
                            )
                            Text(
                                text = icyInfo,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.safeMarquee(),
                            )
                        }
                    }
                }
            }
        }
    }
}
