package com.shapeshed.aerial.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex

/** Stateless adaptive shell for the main destination and its floating content. */
@Composable
internal fun MainAppContent(
    selectedDestination: Int,
    showHome: Boolean,
    onDestinationSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    AdaptiveNavigationShell(
        selectedDestination = selectedDestination,
        showHome = showHome,
        onDestinationSelected = onDestinationSelected,
        modifier = modifier,
    ) {
        Scaffold(
            modifier = Modifier.semantics { traversalIndex = 0f },
            contentWindowInsets = WindowInsets.navigationBars,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding()),
                content = content,
            )
        }
    }
}
