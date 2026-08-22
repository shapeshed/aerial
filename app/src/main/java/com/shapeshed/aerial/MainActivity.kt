package com.shapeshed.aerial

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.shapeshed.aerial.navigation.AerialNavigator
import com.shapeshed.aerial.navigation.AerialRoute
import com.shapeshed.aerial.ui.MainScreen
import com.shapeshed.aerial.ui.MainViewModel
import com.shapeshed.aerial.ui.SettingsScreen
import com.shapeshed.aerial.ui.SettingsViewModel
import com.shapeshed.aerial.ui.StationEditScreen
import com.shapeshed.aerial.ui.StationEditViewModel
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
            initializer { SettingsViewModel(app, app.repository, app.settingsDataStore) }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !mainViewModel.isInitialized.value }
        enableEdgeToEdge()
        setContent {
            AerialTheme {
                val motionScheme = MaterialTheme.motionScheme
                val backStack = rememberNavBackStack(AerialRoute.Main)
                val navigator = remember(backStack) { AerialNavigator(backStack) }
                val repository = remember { (application as AerialApp).repository }

                NavDisplay(
                    backStack = backStack,
                    onBack = { navigator.goBack() },
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    transitionSpec = {
                        (
                            fadeIn(motionScheme.defaultEffectsSpec()) +
                                slideInHorizontally(motionScheme.defaultSpatialSpec()) { (it * 0.15f).toInt() }
                            ) togetherWith
                            fadeOut(motionScheme.defaultEffectsSpec())
                    },
                    popTransitionSpec = {
                        fadeIn(motionScheme.defaultEffectsSpec()) togetherWith
                            (
                                fadeOut(motionScheme.defaultEffectsSpec()) +
                                    slideOutHorizontally(motionScheme.defaultSpatialSpec()) { (it * 0.15f).toInt() }
                            )
                    },
                    predictivePopTransitionSpec = {
                        fadeIn(motionScheme.defaultEffectsSpec()) togetherWith
                            (
                                fadeOut(motionScheme.defaultEffectsSpec()) +
                                    slideOutHorizontally(motionScheme.defaultSpatialSpec()) { (it * 0.15f).toInt() }
                            )
                    },
                    entryProvider = entryProvider {
                        entry<AerialRoute.Main> {
                            MainScreen(
                                viewModel = mainViewModel,
                                onAddStation = dropUnlessResumed { navigator.navigate(AerialRoute.AddStation) },
                                onEditStation = { stationId ->
                                    navigator.navigate(AerialRoute.EditStation(stationId))
                                },
                                onSettings = dropUnlessResumed { navigator.navigate(AerialRoute.Settings) },
                            )
                        }
                        entry<AerialRoute.AddStation> {
                            val vm: StationEditViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { StationEditViewModel(repository, null) }
                                }
                            )
                            StationEditScreen(
                                viewModel = vm,
                                onDismiss = dropUnlessResumed { navigator.goBack() },
                            )
                        }
                        entry<AerialRoute.EditStation> { route ->
                            val vm: StationEditViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { StationEditViewModel(repository, route.stationId) }
                                }
                            )
                            StationEditScreen(
                                viewModel = vm,
                                onDismiss = dropUnlessResumed { navigator.goBack() },
                            )
                        }
                        entry<AerialRoute.Settings> {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onDismiss = dropUnlessResumed { navigator.goBack() },
                            )
                        }
                    },
                )
            }
        }
    }
}
