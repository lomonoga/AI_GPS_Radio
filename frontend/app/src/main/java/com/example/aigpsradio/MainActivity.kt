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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.aigpsradio.navigation.AppNavHost
import com.example.aigpsradio.ui.theme.MyApplicationTheme
import com.example.aigpsradio.viewmodel.LocationViewModel

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private val permissionsGranted = mutableStateOf(false)
    private val openPlayer = mutableStateOf(false) // для навигации по уведомлению

    private var locationViewModel: LocationViewModel? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true

        permissionsGranted.value = fineLocation && coarseLocation && notification

        Log.d(
            TAG,
            "Permissions granted - Fine: $fineLocation, Coarse: $coarseLocation, Notification: $notification"
        )

        if (!permissionsGranted.value) {
            Log.e(TAG, "Some permissions were denied!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d(TAG, "MainActivity onCreate - SDK version: ${Build.VERSION.SDK_INT}")

        locationViewModel = ViewModelProvider(this)[LocationViewModel::class.java]

        // Запрос всех необходимых разрешений
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        // Добавляем разрешение на уведомления для Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            Log.d(TAG, "Added POST_NOTIFICATIONS permission to request list")
        }

        Log.d(TAG, "Requesting permissions: ${permissionsToRequest.joinToString()}")
        permissionLauncher.launch(permissionsToRequest.toTypedArray())

        // Тестовый запрос permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            0
        )

        openPlayer.value = intent?.getBooleanExtra("open_player", false) ?: false
        Log.d(TAG, "onCreate: openPlayer=${openPlayer.value}")

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navHostController = navController,
                    openPlayer = openPlayer.value,
                    locationViewModel = locationViewModel!!
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
        // Останавливаем сервис только при полном закрытии приложения
        if (isFinishing) {
            locationViewModel?.onAppClosing()
            Log.d(TAG, "App closing - ViewModel stopping location service")
        }
    }
}
