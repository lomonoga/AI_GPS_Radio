package com.example.aigpsradio.navigation

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.aigpsradio.ui.InterestsSelectionScreen
import com.example.aigpsradio.ui.PermissionsScreenSimple
import com.example.aigpsradio.ui.VoiceInterestsScreen

@Composable
fun AppNavHost(
    navHostController: NavHostController,
) {
    // Получаем context и читаем флаг первого запуска из SharedPreferences
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val isFirstLaunch = remember { prefs.getBoolean("is_first_launch", true) }

    val startDestination = if (isFirstLaunch) {
        Destination.Permission.route
    } else {
        Destination.VoiceInterests.route
    }

    NavHost(
        navController = navHostController,
        startDestination = startDestination,
    ) {

        composable(route = Destination.Permission.route) {
            PermissionsScreenSimple(
                onContinue = {
                    prefs.edit().putBoolean("is_first_launch", false).apply()
                    navHostController.navigate(Destination.VoiceInterests.route) {
                        popUpTo(Destination.Permission.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Destination.VoiceInterests.route) {
            VoiceInterestsScreen(
                onComplete = {
                    navHostController.navigate(Destination.Player.route)
                },
                onSkip = {
                    navHostController.navigate(Destination.InterestsSelection.route)
                }
            )
        }

        composable(route = Destination.InterestsSelection.route) {
            InterestsSelectionScreen(
                onContinue = {
                    navHostController.navigate(Destination.Player.route)
                }
            )
        }

        composable(route = Destination.Player.route) {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text("Player Screen")
            }
        }
    }
}
