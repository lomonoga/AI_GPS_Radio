package com.example.aigpsradio.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aigpsradio.data.repository.Repository

class ViewModelFactory(
    private val repository: Repository,
    private val application: Application? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AudioStreamViewModel::class.java) -> {
                AudioStreamViewModel(repository) as T
            }
            modelClass.isAssignableFrom(LocationAudioViewModel::class.java) -> {
                requireNotNull(application) { "Application required for LocationAudioViewModel" }
                LocationAudioViewModel(application, repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}