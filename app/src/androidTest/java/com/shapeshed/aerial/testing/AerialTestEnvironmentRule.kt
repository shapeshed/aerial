package com.shapeshed.aerial.testing

import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Provides isolated application state for instrumented tests that use real Aerial services. */
class AerialTestEnvironmentRule : TestWatcher() {
    override fun starting(description: Description) {
        super.starting(description)
        AerialTestEnvironment.app()
        AerialTestEnvironment.resetPreferences()
    }

    override fun finished(description: Description) {
        try {
            AerialTestEnvironment.resetPreferences()
        } finally {
            super.finished(description)
        }
    }
}
