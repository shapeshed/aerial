package com.shapeshed.aerial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shapeshed.aerial.ui.AddStationScreen
import com.shapeshed.aerial.ui.MainScreen
import com.shapeshed.aerial.ui.MainViewModel
import com.shapeshed.aerial.ui.MainViewModelFactory
import com.shapeshed.aerial.ui.RadioDiscoveryViewModel
import com.shapeshed.aerial.ui.SettingsScreen
import com.shapeshed.aerial.ui.SettingsViewModelFactory
import com.shapeshed.aerial.ui.StationEditScreen
import com.shapeshed.aerial.ui.StationEditViewModel
import com.shapeshed.aerial.ui.StationEditViewModelFactory
import com.shapeshed.aerial.ui.theme.AerialTheme

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels {
        val app = application as AerialApp
        MainViewModelFactory(app, app.repository, app.registryRepository, app.settingsDataStore)
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
                val navController = rememberNavController()
                val repository = remember { (application as AerialApp).repository }

                NavHost(
                    navController = navController,
                    startDestination = "main",
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    enterTransition = {
                        fadeIn(motionScheme.defaultEffectsSpec()) +
                            slideInHorizontally(motionScheme.defaultSpatialSpec()) { (it * 0.15f).toInt() }
                    },
                    exitTransition = {
                        fadeOut(motionScheme.defaultEffectsSpec())
                    },
                    popEnterTransition = {
                        fadeIn(motionScheme.defaultEffectsSpec())
                    },
                    popExitTransition = {
                        fadeOut(motionScheme.defaultEffectsSpec()) +
                            slideOutHorizontally(motionScheme.defaultSpatialSpec()) { (it * 0.15f).toInt() }
                    },
                ) {
                    composable("main") {
                        MainScreen(
                            viewModel = mainViewModel,
                            onAddStation = { navController.navigate("station/manual") },
                            onDiscover = { navController.navigate("station/new") },
                            onEditStation = { id -> navController.navigate("station/$id") },
                            onSettings = { navController.navigate("settings") },
                        )
                    }
                    composable("station/new") {
                        val discoverVm: RadioDiscoveryViewModel = viewModel()
                        val showBitrate by mainViewModel.showBitrate.collectAsStateWithLifecycle()
                        AddStationScreen(
                            showBitrate = showBitrate,
                            discoveryViewModel = discoverVm,
                            onAddDiscovered = { station ->
                                mainViewModel.addStation(
                                    name = station.name.trim(),
                                    streamUrl = station.urlResolved.trim(),
                                    logoPath = station.favicon.trim(),
                                    radioBrowserUuid = station.stationuuid.trim(),
                                )
                                navController.popBackStack()
                            },
                            onDismiss = { navController.popBackStack() },
                        )
                    }
                    composable("station/manual") {
                        val vm: StationEditViewModel = viewModel(
                            factory = StationEditViewModelFactory(repository, null)
                        )
                        StationEditScreen(
                            viewModel = vm,
                            onDismiss = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = "station/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments!!.getLong("id")
                        val vm: StationEditViewModel = viewModel(
                            factory = StationEditViewModelFactory(repository, id)
                        )
                        StationEditScreen(
                            viewModel = vm,
                            onDismiss = { navController.popBackStack() },
                        )
                    }
                    composable("settings") {
                        val app = application as AerialApp
                        val vm: com.shapeshed.aerial.ui.SettingsViewModel = viewModel(
                            factory = SettingsViewModelFactory(app, app.repository, app.settingsDataStore)
                        )
                        SettingsScreen(
                            viewModel = vm,
                            onDismiss = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
