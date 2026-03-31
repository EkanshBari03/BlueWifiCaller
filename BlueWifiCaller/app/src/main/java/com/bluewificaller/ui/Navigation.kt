package com.bluewificaller.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.bluewificaller.model.CallState
import com.bluewificaller.ui.screens.*
import com.bluewificaller.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Home    : Screen("home")
    object Peers   : Screen("peers")
    object InCall  : Screen("incall")
    object Incoming: Screen("incoming")
    object Settings: Screen("settings")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val callState by viewModel.callState.collectAsState()

    // Auto-navigate on call state changes
    LaunchedEffect(callState) {
        when (callState) {
            CallState.INCOMING -> navController.navigate(Screen.Incoming.route) {
                launchSingleTop = true
            }
            CallState.OUTGOING, CallState.ACTIVE -> navController.navigate(Screen.InCall.route) {
                launchSingleTop = true
            }
            CallState.ENDED, CallState.IDLE -> {
                if (navController.currentDestination?.route in listOf(
                        Screen.InCall.route, Screen.Incoming.route
                    )) {
                    navController.navigate(Screen.Peers.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPeers = { navController.navigate(Screen.Peers.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                viewModel = viewModel
            )
        }
        composable(Screen.Peers.route) {
            PeersScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
        composable(Screen.InCall.route) {
            InCallScreen(
                viewModel = viewModel
            )
        }
        composable(Screen.Incoming.route) {
            IncomingCallScreen(
                viewModel = viewModel
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
    }
}
