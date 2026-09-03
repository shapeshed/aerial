package com.shapeshed.aerial.testing

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

/** Prevents instrumented tests from ever targeting the developer's personal app install. */
class AerialTestRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle) {
        super.onCreate(arguments)

        check(targetContext.packageName == TEST_APPLICATION_ID) {
            "Aerial instrumented tests must target $TEST_APPLICATION_ID, " +
                "but targeted ${targetContext.packageName}."
        }
    }

    private companion object {
        const val TEST_APPLICATION_ID = "com.shapeshed.aerial.deviceTest"
    }
}
