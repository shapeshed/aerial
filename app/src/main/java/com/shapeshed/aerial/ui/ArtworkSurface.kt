package com.shapeshed.aerial.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Shared Material 3 plate for transparent station artwork.
 *
 * Dark artwork uses a mid-level tonal surface instead of the brightest surface role. Strongly
 * saturated darker artwork can opt into a light plate in dark theme when a dark plate would make
 * its color muddy. Light artwork only gets the inverse surface when a light-theme background
 * would otherwise make it disappear.
 */
@Composable
fun artworkPlateColor(
    isArtworkLight: Boolean,
    prefersLightPlate: Boolean = false,
    hasTransparentMargin: Boolean = false,
): Color {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val needsLightPlate = artworkNeedsLightPlate(
        isDarkTheme,
        isArtworkLight,
        prefersLightPlate,
        hasTransparentMargin,
    )
    val needsDarkPlate = !isDarkTheme && isArtworkLight
    return if (needsLightPlate || needsDarkPlate) {
        MaterialTheme.colorScheme.inverseSurface
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
}

internal fun artworkNeedsLightPlate(
    isDarkTheme: Boolean,
    isArtworkLight: Boolean,
    prefersLightPlate: Boolean,
    hasTransparentMargin: Boolean,
): Boolean = isDarkTheme && ((!isArtworkLight && hasTransparentMargin) || prefersLightPlate)

internal fun artworkNeedsContrastPlate(
    isDarkTheme: Boolean,
    isArtworkLight: Boolean,
    prefersLightPlate: Boolean,
    hasTransparentMargin: Boolean,
): Boolean = artworkNeedsLightPlate(
    isDarkTheme = isDarkTheme,
    isArtworkLight = isArtworkLight,
    prefersLightPlate = prefersLightPlate,
    hasTransparentMargin = hasTransparentMargin,
) || (!isDarkTheme && isArtworkLight)
