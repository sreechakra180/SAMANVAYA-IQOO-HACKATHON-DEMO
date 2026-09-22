package com.sukshma.samanvaya.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sukshma.samanvaya.core.SamanvayaEngine
import com.sukshma.samanvaya.ui.screens.HomeScreen
import com.sukshma.samanvaya.ui.screens.LiveAssistScreen
import com.sukshma.samanvaya.ui.screens.ResultScreen
import com.sukshma.samanvaya.ui.screens.SessionMemoryScreen
import com.sukshma.samanvaya.ui.screens.DiagnosticsScreen

sealed class Screen(val route: String) {
    object Home          : Screen("home")
    object LiveAssist    : Screen("live_assist")
    object Result        : Screen("result/{resultId}") {
        fun withId(id: Long) = "result/$id"
    }
    object SessionMemory : Screen("session_memory")
    object Diagnostics   : Screen("diagnostics")
}

@Composable
fun SamanvayaNavGraph(
    engine: SamanvayaEngine,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStartAssist = { navController.navigate(Screen.LiveAssist.route) },
                onViewMemory  = { navController.navigate(Screen.SessionMemory.route) },
                onDiagnostics = { navController.navigate(Screen.Diagnostics.route) }
            )
        }
        composable(Screen.LiveAssist.route) {
            LiveAssistScreen(
                engine        = engine,
                onBack        = { navController.popBackStack() },
                onResultReady = { id -> navController.navigate(Screen.Result.withId(id)) }
            )
        }
        composable(Screen.Result.route) { backStackEntry ->
            val resultId = backStackEntry.arguments?.getString("resultId")?.toLongOrNull() ?: 0L
            ResultScreen(
                engine   = engine,
                resultId = resultId,
                onBack   = { navController.popBackStack() },
                onMemory = { navController.navigate(Screen.SessionMemory.route) }
            )
        }
        composable(Screen.SessionMemory.route) {
            SessionMemoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Diagnostics.route) {
            DiagnosticsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
