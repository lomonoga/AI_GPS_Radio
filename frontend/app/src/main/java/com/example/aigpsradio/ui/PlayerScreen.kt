package com.example.aigpsradio.ui

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.aigpsradio.model.location.LocationService
import com.example.aigpsradio.ui.theme.MyApplicationTheme


@Composable
fun PlayerScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = {
            Intent(context.applicationContext, LocationService::class.java).apply {
                action = LocationService.ACTION_START

                Log.d(
                    TAG,
                    "Using ContextCompat.startForegroundService() (API ${Build.VERSION.SDK_INT})"
                )
                ContextCompat.startForegroundService(context.applicationContext, this)
            }
        }) {
            Text(text = "Start")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            Log.d(TAG, "Stop button clicked — stopping service")
            Intent(context.applicationContext, LocationService::class.java).apply {
                action = LocationService.ACTION_STOP
                context.startService(this)
                Log.d(TAG, "Requested context.stopService()")
            }
        }) {
            Text(text = "Stop")
        }
    }
}

@Preview(
    showBackground = true,
    device = "id:pixel_6",
    name = "Pixel 6 Light Theme",
)
@Composable
fun PreviewPlayerScreen() {
    MyApplicationTheme(darkTheme = false) {
        PlayerScreen()
    }
}