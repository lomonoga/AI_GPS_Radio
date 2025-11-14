package com.example.aigpsradio

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.aigpsradio.di.NetworkModule
import com.example.aigpsradio.data.repository.Repository
import com.example.aigpsradio.navigation.AppNavHost
import com.example.aigpsradio.ui.theme.MyApplicationTheme
import com.example.aigpsradio.viewmodel.LocationAudioViewModel
import com.example.aigpsradio.viewmodel.LocationViewModel
import com.example.aigpsradio.viewmodel.ViewModelFactory

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private val openPlayer = mutableStateOf(false)
    private var locationViewModel: LocationViewModel? = null
    private var locationAudioViewModel: LocationAudioViewModel? = null

    // Лаунчер для базовых разрешений (геолокация, микрофон, уведомления)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val mic = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true

        Log.d(TAG, "Permissions granted - Fine: $fineLocation, Coarse: $coarseLocation, Mic: $mic, Notification: $notification")

        // Если геолокация предоставлена, запрашиваем фоновую (только для Android 10+)
        if (fineLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    // Отдельный лаунчер для фонового доступа к геолокации (Android 10+)
    private val backgroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Background location permission granted: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d(TAG, "MainActivity onCreate - SDK version: ${Build.VERSION.SDK_INT}")

        // Создаем Repository и Factory
        val repository = Repository(NetworkModule.api)
        val factory = ViewModelFactory(repository, application)

        // Создаем ViewModels с использованием factory
        locationViewModel = ViewModelProvider(this)[LocationViewModel::class.java]
        locationAudioViewModel = ViewModelProvider(this, factory)[LocationAudioViewModel::class.java]

        // Запрос всех необходимых разрешений
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )

        // Добавляем разрешение на уведомления для Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            Log.d(TAG, "Added POST_NOTIFICATIONS permission to request list")
        }

        Log.d(TAG, "Requesting permissions: ${permissionsToRequest.joinToString()}")
        permissionLauncher.launch(permissionsToRequest.toTypedArray())

        openPlayer.value = intent?.getBooleanExtra("open_player", false) ?: false
        Log.d(TAG, "onCreate: openPlayer=${openPlayer.value}")

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navHostController = navController,
                    openPlayer = openPlayer.value,
                    locationViewModel = locationViewModel!!,
                    locationAudioViewModel = locationAudioViewModel!!
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newOpenPlayer = intent.getBooleanExtra("open_player", false)
        Log.d(TAG, "onNewIntent: openPlayer=$newOpenPlayer")
        openPlayer.value = newOpenPlayer
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            locationViewModel?.onAppClosing()
            Log.d(TAG, "App closing - ViewModel stopping location service")
        }
    }
}