package com.example.aigpsradio.model.location

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.aigpsradio.MainActivity
import com.example.aigpsradio.R
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


private const val TAG = "LocationService"

class LocationService: Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    private var lastLocation: Location? = null  // хранит последнее полученное местоположение пользователя

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
        Log.d(TAG, "LocationService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    @SuppressLint("NotificationPermission")
    private fun start() {

        Log.d(TAG, "Starting location tracking")

        // PendingIntent для открытия приложения при клике на уведомление
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("open_player", true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Отслеживание местоположения...")
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        locationClient
            .getLocationUpdates(10000L)   // Обновление каждые 10 секунд
            .catch { e ->
                Log.e(TAG, "Location error: ${e.message}")
                e.printStackTrace()
            }
            .onEach { location ->
                lastLocation = location

                val lat = "%.6f".format(location.latitude)
                val long = "%.6f".format(location.longitude)
                val accuracy = "%.1f".format(location.accuracy)

                val updatedNotification = notification
                    .setContentText("Location: ($lat, $long)")
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("📍 Широта: $lat, Долгота: $long\n" + "🎯 Точность: $accuracy м"))

                notificationManager.notify(NOTIFICATION_ID, updatedNotification.build())

                // Отправка broadcast для обновления UI
                sendLocationBroadcast(location)
            }
            .launchIn(serviceScope)

        startForeground(NOTIFICATION_ID, notification.build())
    }

    private fun sendLocationBroadcast(location: Location) {
        val intent = Intent(ACTION_LOCATION_UPDATE).apply {
            putExtra(EXTRA_LOCATION_LAT, location.latitude)
            putExtra(EXTRA_LOCATION_LON, location.longitude)
            putExtra(EXTRA_LOCATION_ACCURACY, location.accuracy)
        }
        sendBroadcast(intent)
    }

    private fun stop() {
        Log.d(TAG, "Stopping location tracking")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "LocationService destroyed")
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_LOCATION_UPDATE = "com.example.aigpsradio.LOCATION_UPDATE"
        const val EXTRA_LOCATION_LAT = "EXTRA_LOCATION_LAT"
        const val EXTRA_LOCATION_LON = "EXTRA_LOCATION_LON"
        const val EXTRA_LOCATION_ACCURACY = "EXTRA_LOCATION_ACCURACY"
        private const val NOTIFICATION_ID = 1
    }
}