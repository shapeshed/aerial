package com.shapeshed.aerial.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LogoPathRecoveryTest {
    @Test
    fun staleCachedLogoFallsBackToPersistedRemoteUrlAfterReinstall() {
        assertEquals(
            "https://cdn.example.test/station.svg",
            recoverLogoPath(
                storedPath = "/data/user/0/com.shapeshed.aerial/files/logos/station.svg",
                remoteLogoUrl = "https://cdn.example.test/station.svg",
                fileExists = false,
            ),
        )
    }

    @Test
    fun existingEditedLocalLogoRemainsTheSourceOfTruth() {
        val editedLogo = "/data/user/0/com.shapeshed.aerial/files/logos/my-edited-logo.png"

        assertEquals(
            editedLogo,
            recoverLogoPath(
                storedPath = editedLogo,
                remoteLogoUrl = "https://cdn.example.test/original.svg",
                fileExists = true,
            ),
        )
    }
}
