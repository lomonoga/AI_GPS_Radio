package com.example.aigpsradio.model.location

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import org.osmdroid.config.Configuration

class LocationApp: Application() {

    override fun onCreate() {
        super.onCreate()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(     // Создание канала уведомлений
                "location",
                "Location",
                NotificationManager.IMPORTANCE_LOW // Важность уведомлений (низкая)
            ).apply {
                description = "Shows your current location while tracking"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Инициализация конфигурации OSM при запуске приложения
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osm_pref", Context.MODE_PRIVATE)
        )

        // Установка User Agent
        Configuration.getInstance().userAgentValue = packageName

        // Настройка кэша карт
        Configuration.getInstance().apply {
            osmdroidBasePath = getExternalFilesDir(null)  // Корневая папка для всего кроме тайлов
            osmdroidTileCache = getExternalFilesDir("osmdroid/tiles") // Подпапка для тайлов
        }
    }
}