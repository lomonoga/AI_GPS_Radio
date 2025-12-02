package com.example.aigpsradio.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aigpsradio.data.repository.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class AudioStreamUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val audioFile: File? = null,
    val isPlaying: Boolean = false
)

class AudioStreamViewModel(
    private val repository: Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AudioStreamUiState())
    val uiState: StateFlow<AudioStreamUiState> = _uiState.asStateFlow()


    fun loadAudio(s3key: String, context: Context) {
        // Передача Context в метод ViewModel — антипаттерн
        // если пользователь вызвал loadAudio несколько раз, предыдущая корутина продолжит выполняться - плохо

        viewModelScope.launch {
            // атомарное обновление состояния
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Получаем аудио с сервера
            repository.streamAudio(s3key).onSuccess { responseBody ->
                // Сохраняем в кеш
                repository.saveAudioToCache(responseBody, context.cacheDir)
                    .onSuccess { file ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            audioFile = file,
                            isPlaying = true
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Save error: ${error.localizedMessage}"
                        )
                    }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Network error: ${error.localizedMessage}"
                )
            }
        }
    }

    fun stopAudio() {
        _uiState.update {
            it.copy(
                audioFile = null,
                isPlaying = false
            )
        }
    }
}