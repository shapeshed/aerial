package com.shapeshed.aerial

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.remember
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shapeshed.aerial.ui.MainScreen
import com.shapeshed.aerial.ui.MainViewModel
import com.shapeshed.aerial.ui.SettingsScreen
import com.shapeshed.aerial.ui.SettingsViewModel
import com.shapeshed.aerial.ui.StationEditScreen
import com.shapeshed.aerial.ui.StationEditViewModel
import com.shapeshed.aerial.ui.ZipSettingsBackupManager
import com.shapeshed.aerial.ui.theme.AerialTheme

class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels {
        val app = application as AerialApp
        viewModelFactory {
            initializer {
                MainViewModel(app, app.repository, app.registryRepository, app.settingsDataStore, createSavedStateHandle())
            }
        }
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        val app = application as AerialApp
        viewModelFactory {
            initializer {
                SettingsViewModel(
                    app,
                    app.settingsDataStore,
                    ZipSettingsBackupManager(app, app.repository, app.settingsDataStore),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !mainViewModel.isInitialized.value }
        enableEdgeToEdge()
        setContent {
            AerialTheme {
                val repository = remember { (application as AerialApp).repository }
                val registryRepository = remember { (application as AerialApp).registryRepository }

                MainScreen(
                    viewModel = mainViewModel,
                    settingsContent = { onDismiss ->
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onDismiss = dropUnlessResumed { onDismiss() },
                        )
                    },
                    stationEditContent = { stationId, onDismiss ->
                        val vm: StationEditViewModel = viewModel(
                            factory = viewModelFactory {
                                initializer {
                                    StationEditViewModel(
                                        repository,
                                        registryRepository,
                                        stationId,
                                    )
                                }
                            }
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
