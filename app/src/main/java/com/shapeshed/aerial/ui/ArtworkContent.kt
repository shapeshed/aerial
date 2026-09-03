package com.shapeshed.aerial.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.Station
import java.io.File

internal fun String.avatarInitial(): String {
    val trimmed = trim()
    if (trimmed.isEmpty()) return ""
    val charCount = Character.charCount(trimmed.codePointAt(0))
    return trimmed.substring(0, charCount).uppercase()
}

// A logo path is either a remote URL or a local file path — the latter needs wrapping in a
// File so Coil resolves it, rather than trying (and failing) to treat it as a bare URI string.
// Recently-played entries in particular can carry a local path here: they're backed by
// RegistryStation, but MainViewModel substitutes a locally-saved station's own logoPath when
// the registry's copy has none.
internal fun logoModelFor(path: String): Any? = when {
    path.startsWith("http") -> path
    path.isNotEmpty() -> File(path)
    else -> null
}

@Composable
internal fun ForYouStationCard(
    station: com.shapeshed.aerial.data.RegistryStation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Match the flat tonal containment used by the favourites station tiles.
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier.width(140.dp).height(116.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp).fillMaxSize(),
        ) {
            StationLogoCircle(
                logoModel = logoModelFor(station.logoUrl),
                size = 64.dp,
            ) {
                Text(
                    text = station.name.avatarInitial(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Circular station logo on a plate. The plate shows through transparent regions of third-party
// artwork so every logo sits on a consistent background, and it is never a visible ring because
// the artwork fills the circle. Stations without a usable logo keep a tonal circle behind the
// fallback content instead of the plate.
@Composable
fun StationLogoCircle(
    logoModel: Any?,
    size: Dp,
    modifier: Modifier = Modifier,
    fallbackBackground: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    opaqueArtworkBackground: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainer,
    fallback: @Composable () -> Unit,
) {
    StationLogoContent(
        logoModel = logoModel,
        modifier = modifier.size(size),
        fallbackBackground = fallbackBackground,
        opaqueArtworkBackground = opaqueArtworkBackground,
        fallback = fallback,
    )
}

@Composable
internal fun StationLogoContent(
    logoModel: Any?,
    modifier: Modifier,
    fallbackBackground: androidx.compose.ui.graphics.Color,
    opaqueArtworkBackground: androidx.compose.ui.graphics.Color,
    fallback: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }
    var logoFailed by remember(logoModel) { mutableStateOf(false) }
    var logoIsLight by remember(logoModel) { mutableStateOf(false) }
    var logoPrefersLightPlate by remember(logoModel) { mutableStateOf(false) }
    var logoHasTransparentMargin by remember(logoModel) { mutableStateOf(false) }
    var loadedLogo by remember(logoModel) { mutableStateOf<coil3.Image?>(null) }
    LaunchedEffect(logoModel, loadedLogo) {
        val image = loadedLogo ?: return@LaunchedEffect
        logoIsLight = sharedLogoAppearanceAnalyzer
            .analyze(logoModel.toString(), image)
            .also {
                logoPrefersLightPlate = it.prefersLightPlate
                logoHasTransparentMargin = it.hasTransparentMargin
            }
            .isLight
    }
    val showLogo = logoModel != null && !logoFailed
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(
                // The outer circle is the row's tonal surface; adaptive artwork color is
                // applied only to the inset image circle below so the border remains visible.
                fallbackBackground,
            ),
    ) {
        if (showLogo) {
            AsyncImage(
                model = logoModel,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onError = { logoFailed = true },
                onSuccess = { state -> loadedLogo = state.result.image },
                modifier = Modifier
                    .fillMaxSize(GRID_LOGO_INSET_FRACTION)
                    .clip(CircleShape)
                    .background(
                        if (logoHasTransparentMargin) {
                            artworkPlateColor(
                                logoIsLight,
                                logoPrefersLightPlate,
                                hasTransparentMargin = true,
                            )
                        } else {
                            opaqueArtworkBackground
                        },
                        CircleShape,
                    ),
            )
        } else {
            fallback()
        }
    }
}

@Composable
fun StationAvatar(
    station: Station,
    isActive: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
    surfaceColor: androidx.compose.ui.graphics.Color? = null,
    artworkSurfaceColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    val logoModel = logoModelFor(station.logoPath)
    StationLogoCircle(
        logoModel = logoModel,
        size = size,
        modifier = modifier,
        opaqueArtworkBackground = artworkSurfaceColor,
        fallbackBackground = surfaceColor ?: if (isActive) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Icon(
            imageVector = Icons.Rounded.Radio,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}
