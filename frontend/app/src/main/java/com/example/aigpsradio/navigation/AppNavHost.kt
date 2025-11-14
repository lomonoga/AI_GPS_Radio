package com.example.aigpsradio.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.aigpsradio.ui.InterestsSelectionScreen
import com.example.aigpsradio.ui.PermissionsScreenSimple
import com.example.aigpsradio.ui.PlayerScreen
import com.example.aigpsradio.ui.VoiceInterestsScreen
import com.example.aigpsradio.viewmodel.LocationViewModel
import com.example.aigpsradio.viewmodel.LocationAudioViewModel

@Composable
fun AppNavHost(
    navHostController: NavHostController,
    openPlayer: Boolean,
    locationViewModel: LocationViewModel,
    locationAudioViewModel: LocationAudioViewModel
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val isFirstLaunch = remember { prefs.getBoolean("is_first_launch", true) }

    // Триггер для принудительного обновления
    var refreshTrigger by remember { mutableStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Отслеживаем возврат к приложению для обновления статуса разрешений
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Функции проверки разрешений
    fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkBackgroundPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun checkMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkNotifsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    val permissionsGranted = checkLocationPermission() &&
            checkBackgroundPermission() &&
            checkMicPermission() &&
            checkNotifsPermission()

    val startDestination = if (!permissionsGranted || isFirstLaunch) {
        Destination.Permission.route
    } else {
        Destination.VoiceInterests.route
    }

    NavHost(
        navController = navHostController,
        startDestination = startDestination,
    ) {

        composable(route = Destination.Permission.route) {
            // Перечитываем статус разрешений при каждом refreshTrigger
            val locationGranted = remember(refreshTrigger) { checkLocationPermission() }
            val backgroundGranted = remember(refreshTrigger) { checkBackgroundPermission() }
            val micGranted = remember(refreshTrigger) { checkMicPermission() }
            val notifsGranted = remember(refreshTrigger) { checkNotifsPermission() }

            PermissionsScreenSimple(
                locationGranted = locationGranted,
                backgroundGranted = backgroundGranted,
                micGranted = micGranted,
                notifsGranted = notifsGranted,
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
            PlayerScreen(
                locationviewModel = locationViewModel,
                locationAudioViewModel = locationAudioViewModel
            )
        }
    }

    LaunchedEffect(openPlayer) {
        if (openPlayer) {
            navHostController.navigate(Destination.Player.route) {
                popUpTo(navHostController.graph.startDestinationId) { inclusive = false }
            }
        }
    }
}