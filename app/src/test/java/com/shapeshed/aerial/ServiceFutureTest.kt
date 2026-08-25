package com.shapeshed.aerial

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceFutureTest {
    @Test
    fun scopeCancellationCancelsTheMediaSessionFuture() = runTest {
        val future = backgroundScope.asServiceFuture {
            awaitCancellation()
        }

        backgroundScope.cancel()
        runCurrent()

        assertTrue(future.isCancelled)
    }
}
