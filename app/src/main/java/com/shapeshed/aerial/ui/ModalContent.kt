package com.shapeshed.aerial.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.Station

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun MainModalHost(
    showCountrySheet: Boolean,
    showGenreSheet: Boolean,
    countrySheetState: androidx.compose.material3.SheetState,
    genreSheetState: androidx.compose.material3.SheetState,
    countryQuery: String,
    genreQuery: String,
    availableCountries: List<String>,
    selectedCountries: Set<String>,
    allTags: List<String>,
    selectedTags: Set<String>,
    tagLabels: Map<String, String>,
    appLocale: java.util.Locale,
    contextStation: Station?,
    stationToDelete: Station?,
    onDismissCountry: () -> Unit,
    onDismissGenre: () -> Unit,
    onCountryQueryChange: (String) -> Unit,
    onGenreQueryChange: (String) -> Unit,
    onToggleCountry: (String) -> Unit,
    onClearCountry: () -> Unit,
    onToggleTag: (String) -> Unit,
    onClearTag: () -> Unit,
    onDismissContext: () -> Unit,
    onEditStation: (Station) -> Unit,
    onRequestDelete: (Station) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (Station) -> Unit,
) {
    if (showCountrySheet) {
        ModalBottomSheet(onDismissRequest = onDismissCountry, sheetState = countrySheetState) {
            FilterPickerSheetContent(
                title = stringResource(R.string.filter_country),
                searchLabel = stringResource(R.string.search_countries),
                query = countryQuery,
                onQueryChange = onCountryQueryChange,
                items = availableCountries,
                selectedItems = selectedCountries,
                displayName = { countryName(it, appLocale) },
                onToggle = onToggleCountry,
                onSelectionComplete = onDismissCountry,
                onClear = onClearCountry,
            )
        }
    }
    if (showGenreSheet) {
        ModalBottomSheet(onDismissRequest = onDismissGenre, sheetState = genreSheetState) {
            FilterPickerSheetContent(
                title = stringResource(R.string.filter_genre),
                searchLabel = stringResource(R.string.search_genres),
                query = genreQuery,
                onQueryChange = onGenreQueryChange,
                items = allTags,
                selectedItems = selectedTags,
                displayName = { tagLabels[it] ?: it },
                onToggle = onToggleTag,
                onSelectionComplete = onDismissGenre,
                onClear = onClearTag,
            )
        }
    }
    contextStation?.let { station ->
        ModalBottomSheet(
            onDismissRequest = onDismissContext,
            sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = SHEET_ENABLED_VALUES,
            ),
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            StationContextSheet(
                station = station,
                onEdit = { onEditStation(station) },
                onDelete = { onRequestDelete(station) },
            )
        }
    }
    stationToDelete?.let { station ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text(stringResource(R.string.remove_station_title)) },
            text = { Text(stringResource(R.string.remove_station_message, station.name)) },
            confirmButton = {
                TextButton(onClick = { onConfirmDelete(station) }) {
                    Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

// Pre-search browse list (A-Z subsection of the registry) shown on a cold start when there
// are no recent searches, so the search view isn't empty before the user types.
@Composable
internal fun SearchFilterRow(
    selectedCountries: Set<String>,
    selectedTags: Set<String>,
    onCountryClick: () -> Unit,
    onGenreClick: () -> Unit,
    onClearAll: () -> Unit,
    hasFilters: Boolean,
    modifier: Modifier = Modifier,
) {
    val appLocale = LocalConfiguration.current.locales[0]
    val tagLabels = rememberTagLabels()
    fun chipLabel(selected: Set<String>, fallback: String, displayName: (String) -> String = { it }): String = when (selected.size) {
        0 -> fallback
        1 -> displayName(selected.first())
        else -> "${displayName(selected.first())}+${selected.size - 1}"
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        FilterChip(
            selected = selectedCountries.isNotEmpty(),
            onClick = onCountryClick,
            label = { Text(chipLabel(selectedCountries, stringResource(R.string.filter_country)) { countryName(it, appLocale) }) },
            leadingIcon = if (selectedCountries.isNotEmpty()) {
                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            } else null,
            trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
        FilterChip(
            selected = selectedTags.isNotEmpty(),
            onClick = onGenreClick,
            label = { Text(chipLabel(selectedTags, stringResource(R.string.filter_genre)) { tagLabels[it] ?: it }) },
            leadingIcon = if (selectedTags.isNotEmpty()) {
                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            } else null,
            trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
        if (hasFilters) {
            TextButton(onClick = onClearAll) { Text(stringResource(R.string.clear_all)) }
        }
    }
}

@Composable
private fun StationContextSheet(
    station: Station,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Text(
            text = station.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        HorizontalDivider()
        ListItem(
            leadingContent = {
                Icon(Icons.Rounded.Edit, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onEdit),
        ) {
            Text(stringResource(R.string.action_edit))
        }
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            modifier = Modifier.clickable(onClick = onDelete),
        ) {
            Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(8.dp))
    }
}

// Grapheme-safe "first letter" for a station-name fallback avatar. take(1) slices by UTF-16
// code unit, not code point — a name starting with a supplementary-plane character (some
// emoji, rare CJK extensions) would cut a lone surrogate half and render as a broken glyph.
// codePointAt/charCount takes the full code point regardless of whether it's one or two chars.
