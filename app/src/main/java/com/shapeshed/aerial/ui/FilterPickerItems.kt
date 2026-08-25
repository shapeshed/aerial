package com.shapeshed.aerial.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shapeshed.aerial.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FilterPickerItem(
    val value: String,
    val label: String,
    val selected: Boolean,
)

internal fun filterPickerItems(
    items: List<String>,
    selectedItems: Set<String>,
    query: String,
    displayName: (String) -> String,
): List<FilterPickerItem> {
    val normalizedQuery = query.trim()
    return items
        .map { value ->
            FilterPickerItem(
                value = value,
                label = displayName(value),
                selected = value in selectedItems,
            )
        }
        .filter { normalizedQuery.isBlank() || it.label.contains(normalizedQuery, ignoreCase = true) }
        .sortedWith(compareByDescending<FilterPickerItem> { it.selected }.thenBy { it.label })
}

@Composable
internal fun FilterPickerSheetContent(
    title: String,
    searchLabel: String,
    query: String,
    onQueryChange: (String) -> Unit,
    items: List<String>,
    selectedItems: Set<String>,
    displayName: (String) -> String,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectionComplete: () -> Unit = {},
) {
    val filteredItems by produceState<List<FilterPickerItem>?>(null, items, selectedItems, query, displayName) {
        value = withContext(Dispatchers.Default) {
            filterPickerItems(items, selectedItems, query, displayName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, bottom = 4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (selectedItems.isNotEmpty()) {
                TextButton(onClick = onClear) { Text(stringResource(R.string.action_clear)) }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text(searchLabel) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_search))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        )
        val visibleItems = filteredItems
        if (visibleItems == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().weight(1f)) {
                CircularProgressIndicator()
            }
        } else if (visibleItems.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().weight(1f)) {
                HomeEmptyState(
                    text = stringResource(R.string.no_matches),
                    supportingText = stringResource(R.string.no_matches_desc),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(visibleItems, key = { it.value }) { item ->
                    ListItem(
                        modifier = Modifier.clickable {
                            onToggle(item.value)
                            onSelectionComplete()
                        },
                        trailingContent = { Checkbox(checked = item.selected, onCheckedChange = null) },
                    ) {
                        Text(item.label)
                    }
                }
            }
        }
    }
}
