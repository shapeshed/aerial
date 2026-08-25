package com.shapeshed.aerial.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex

/** Stateless Material scaffold for a Home, Favorites, or mood destination. */
@Composable
internal fun MainAppContent(
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
    topBar: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier.semantics { traversalIndex = 0f },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = topBar,
        snackbarHost = snackbarHost,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            content = content,
        )
    }
}
