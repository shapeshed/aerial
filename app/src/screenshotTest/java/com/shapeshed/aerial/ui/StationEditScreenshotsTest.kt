package com.shapeshed.aerial.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shapeshed.aerial.ui.theme.AerialTheme

@PreviewTest
@Preview(name = "Add station", device = "spec:width=400dp,height=800dp,dpi=420")
@Composable
private fun AddStationScreenshot() {
    AerialTheme(dynamicColor = false) {
        StationEditContent(
            isEditing = false,
            name = "",
            streamUrl = "",
            logoModel = null,
            showRemoveLogo = false,
            showRemoveLogoConfirm = false,
            onNameChange = {},
            onStreamUrlChange = {},
            onChangeLogo = {},
            onRequestRemoveLogo = {},
            onRemoveLogo = {},
            onDismissRemoveLogo = {},
            onSave = {},
            onDismiss = {},
        )
    }
}

@PreviewTest
@Preview(name = "Edit station", device = "spec:width=400dp,height=800dp,dpi=420")
@Composable
private fun EditStationScreenshot() {
    AerialTheme(dynamicColor = false) {
        StationEditContent(
            isEditing = true,
            name = "Radio Paradise",
            streamUrl = "https://stream.radioparadise.com/mp3-192",
            logoModel = null,
            showRemoveLogo = true,
            showRemoveLogoConfirm = false,
            onNameChange = {},
            onStreamUrlChange = {},
            onChangeLogo = {},
            onRequestRemoveLogo = {},
            onRemoveLogo = {},
            onDismissRemoveLogo = {},
            onSave = {},
            onDismiss = {},
        )
    }
}
