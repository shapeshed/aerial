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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shapeshed.aerial.ui.MainScreen
import com.shapeshed.aerial.ui.MainViewModel
import com.shapeshed.aerial.ui.MainViewModelFactory
import com.shapeshed.aerial.ui.SettingsScreen
import com.shapeshed.aerial.ui.SettingsViewModelFactory
import com.shapeshed.aerial.ui.StationEditScreen
import com.shapeshed.aerial.ui.StationEditViewModel
import com.shapeshed.aerial.ui.StationEditViewModelFactory
import com.shapeshed.aerial.ui.theme.AerialTheme

object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val STATION_ADD = "station/manual"
    const val STATION_EDIT = "station/{stationId}"
    fun stationEdit(id: Long) = "station/$id"
}

class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels {
        val app = application as AerialApp
        MainViewModelFactory(app, app.repository, app.registryRepository, app.settingsDataStore)
    }

    private val settingsViewModel: com.shapeshed.aerial.ui.SettingsViewModel by viewModels {
        val app = application as AerialApp
        com.shapeshed.aerial.ui.SettingsViewModelFactory(app, app.repository, app.settingsDataStore)
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
                    startDestination = Routes.MAIN,
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
                    composable(Routes.MAIN) {
                        MainScreen(
                            viewModel = mainViewModel,
                            onAddStation = { navController.navigate(Routes.STATION_ADD) },
                            onEditStation = { stationId -> navController.navigate(Routes.stationEdit(stationId)) },
                            onSettings = { navController.navigate(Routes.SETTINGS) },
                        )
                    }
                    composable(Routes.STATION_ADD) {
                        val vm: StationEditViewModel = viewModel(
                            factory = StationEditViewModelFactory(repository, null)
                        )
                        StationEditScreen(
                            viewModel = vm,
                            onDismiss = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = Routes.STATION_EDIT,
                        arguments = listOf(navArgument("stationId") { type = NavType.LongType }),
                    ) { backStackEntry ->
                        val stationId = backStackEntry.arguments?.getLong("stationId")
                        val vm: StationEditViewModel = viewModel(
                            factory = StationEditViewModelFactory(repository, stationId)
                        )
                        StationEditScreen(
                            viewModel = vm,
                            onDismiss = { navController.popBackStack() },
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onDismiss = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
