package com.shapeshed.aerial.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shapeshed.aerial.data.SleepTimerState

private const val MINUTE_MS = 60_000L

val SLEEP_TIMER_PRESETS_MS: List<Long> = listOf(
    30_000L,
    15 * MINUTE_MS,
    30 * MINUTE_MS,
    45 * MINUTE_MS,
    60 * MINUTE_MS,
    90 * MINUTE_MS,
)

/** Formats remaining milliseconds as H:MM:SS (or M:SS under an hour), rounding seconds up. */
fun formatSleepRemaining(ms: Long): String {
    val totalSec = ((ms + 999) / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun presetLabel(ms: Long): String {
    if (ms < MINUTE_MS) return "${ms / 1000} sec"
    val min = ms / MINUTE_MS
    return when {
        min < 60 -> "$min min"
        min % 60 == 0L -> "${min / 60} hr"
        else -> "${min / 60}h ${min % 60}m"
    }
}

/** Now Playing top-bar action: a plain icon when idle, a tonal icon with countdown when active. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SleepTimerAction(active: SleepTimerState?, onClick: () -> Unit) {
    if (active != null) {
        // Active state: a filled Material You accent pill (primaryContainer) holding the icon
        // and live countdown. The whole pill opens the picker, and it's inset to sit at the
        // 16.dp margin used by the mini-player lozenge.
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            // End inset so the pill's right edge lines up with the 24.dp content margin of the
            // main artwork below it.
            modifier = Modifier.padding(end = 20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Icon(
                    Icons.Rounded.Bedtime,
                    contentDescription = "Sleep timer",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatSleepRemaining(active.remainingMs),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    } else {
        // Match the active pill's inset so the idle icon isn't flush against the edge and
        // lines up with the 24.dp artwork margin.
        IconButton(
            onClick = onClick,
            shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
            modifier = Modifier.padding(end = 8.dp),
        ) {
            Icon(Icons.Rounded.Bedtime, contentDescription = "Sleep timer")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun SleepTimerSheet(
    active: SleepTimerState?,
    onSet: (Long) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
        ) {
            Text(
                text = "Sleep timer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            if (active != null) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = formatSleepRemaining(active.remainingMs),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(16.dp))
                // Elapsed fraction so the bar fills left-to-right as the timer counts down
                // (empty at the start, full when it's about to fire).
                val fraction = if (active.totalMs > 0) {
                    (1f - active.remainingMs.toFloat() / active.totalMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = tween(500),
                    label = "sleepProgress",
                )
                LinearWavyProgressIndicator(
                    progress = { animatedFraction },
                    // Keep it wavy throughout — the default amplitude flattens the wave near
                    // progress 0 and 1, which looked like an intermittent bug.
                    amplitude = { 1f },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WavyProgressIndicatorDefaults.LinearContainerHeight),
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { onCancel(); onDismiss() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel")
                    }
                    FilledTonalButton(
                        onClick = { onSet(active.remainingMs + 15 * MINUTE_MS) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("+15 min")
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "Set a new duration",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))
            // M3 Expressive single-select: toggle buttons that morph round -> square and fill
            // with the primary (Material You) colour when selected. FlowRow wraps so every
            // preset stays visible rather than collapsing into an overflow menu.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SLEEP_TIMER_PRESETS_MS.forEach { ms ->
                    ToggleButton(
                        checked = active?.totalMs == ms,
                        onCheckedChange = { onSet(ms); onDismiss() },
                        // Visual hierarchy per applying-m-3-expressive: unselected siblings are
                        // squarer and the selected one rounds out to stand apart.
                        shapes = ToggleButtonDefaults.shapes(
                            shape = ToggleButtonDefaults.squareShape,
                            pressedShape = ToggleButtonDefaults.pressedShape,
                            checkedShape = ToggleButtonDefaults.roundShape,
                        ),
                        // Larger touch target — these are primary actions.
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    ) {
                        Text(presetLabel(ms), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
