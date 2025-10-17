package com.example.aigpsradio

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.rememberNavController
import com.example.aigpsradio.navigation.AppNavHost
import com.example.aigpsradio.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private val permissionsGranted = mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true

        permissionsGranted.value = fineLocation && coarseLocation && notification

        Log.d(TAG, "Permissions granted - Fine: $fineLocation, Coarse: $coarseLocation, Notification: $notification")

        if (!permissionsGranted.value) {
            Log.e(TAG, "Some permissions were denied!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d(TAG, "MainActivity onCreate - SDK version: ${Build.VERSION.SDK_INT}")

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


        setContent {
            MyApplicationTheme {

                val navController = rememberNavController()
                AppNavHost(navHostController = navController)

            }
        }
    }
}

//enum class Screen {
//    Permissions,
//    InterestsSelection,
//    VoiceInterests,
//    MainApp
//}