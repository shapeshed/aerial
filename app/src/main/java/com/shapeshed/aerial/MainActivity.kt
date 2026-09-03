package com.shapeshed.aerial

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shapeshed.aerial.ui.MainScreen
import com.shapeshed.aerial.ui.MainViewModel
import com.shapeshed.aerial.ui.SettingsScreen
import com.shapeshed.aerial.ui.SettingsViewModel
import com.shapeshed.aerial.ui.StationEditScreen
import com.shapeshed.aerial.ui.StationEditViewModel
import com.shapeshed.aerial.ui.importStationLogo
import com.shapeshed.aerial.ui.theme.AerialTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !mainViewModel.isInitialized.value }
        enableEdgeToEdge()
        setContent {
            AerialTheme {
                MainScreen(
                    viewModel = mainViewModel,
                    settingsContent = { onDismiss ->
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onDismiss = dropUnlessResumed { onDismiss() },
                        )
                    },
                    stationEditContent = { stationId, onDismiss ->
                        val vm: StationEditViewModel = hiltViewModel(
                            creationCallback = { factory: StationEditViewModel.Factory ->
                                factory.create(stationId, ::importStationLogo)
                            },
                        )
                        StationEditScreen(
                            viewModel = vm,
                            onDismiss = dropUnlessResumed { onDismiss() },
                        )
                    },
                )
            }
        }
    }
}
