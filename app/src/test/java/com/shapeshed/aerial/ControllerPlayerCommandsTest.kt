package com.shapeshed.aerial

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Player.Commands needs Android's SparseBooleanArray, which is stubbed in JVM tests, so the
// grant decision is tested here and the resulting command set is exercised on device.
class ControllerPlayerCommandsTest {

    @Test
    fun untrustedPlatformControllerLikeGarminConnectGetsTransportCommands() {
        // Log from a fenix 7 via Garmin Connect: trusted=false version=0.
        assertTrue(grantsTransportCommands(isTrusted = false, controllerVersion = 0))
    }

    @Test
    fun untrustedMedia3ControllerStaysReadOnly() {
        assertFalse(grantsTransportCommands(isTrusted = false, controllerVersion = 1_011_001_300))
    }

    @Test
    fun trustedControllerUsesTheFullDefaultsInstead() {
        assertFalse(grantsTransportCommands(isTrusted = true, controllerVersion = 0))
    }
}
