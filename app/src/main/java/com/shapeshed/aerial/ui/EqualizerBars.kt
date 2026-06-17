package com.shapeshed.aerial.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

private val BAR_DURATIONS = listOf(380, 440, 310, 500)
private val BAR_OFFSETS = listOf(0, 120, 60, 200)

@Composable
fun EqualizerBars(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 4,
) {
    val count = barCount.coerceIn(1, BAR_DURATIONS.size)
    val infiniteTransition = rememberInfiniteTransition(label = "eq")

    val amplitude by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "amplitude",
    )

    val heights = List(count) { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = BAR_DURATIONS[i], easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(BAR_OFFSETS[i]),
            ),
            label = "bar$i",
        )
    }

    Canvas(modifier = modifier) {
        val gap = if (count > 1) size.width * 0.15f / (count - 1) else 0f
        val barWidth = (size.width - gap * (count - 1)) / count

        heights.forEachIndexed { i, h ->
            val frac = 0.2f + (h.value - 0.2f) * amplitude
            val barH = size.height * frac
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), size.height - barH),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}
