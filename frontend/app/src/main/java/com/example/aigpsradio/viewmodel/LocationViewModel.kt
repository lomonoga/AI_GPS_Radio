package com.example.aigpsradio.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aigpsradio.model.location.LocationService
import kotlinx.coroutines.launch

private const val TAG = "LocationViewModel"

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private var isServiceRunning = false

    fun startLocationTracking() {
        if (!isServiceRunning) {
            viewModelScope.launch {
                Intent(getApplication(), LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    ContextCompat.startForegroundService(getApplication(), this)
                }
                isServiceRunning = true
                Log.d(TAG, "Location tracking started")
            }
        } else {
            Log.d(TAG, "Location tracking already running")
        }
    }

    fun stopLocationTracking() {
        if (isServiceRunning) {
            Intent(getApplication(), LocationService::class.java).apply {
                action = LocationService.ACTION_STOP
                getApplication<Application>().startService(this)
            }
            isServiceRunning = false
            Log.d(TAG, "Location tracking stopped")
        }
    }
    // Вызывается когда Activity полностью уничтожается (приложение закрывается)
    fun onAppClosing() {
        Log.d(TAG, "App is closing, stopping location service")
        stopLocationTracking()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
    }
}