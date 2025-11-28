package com.example.aigpsradio.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.aigpsradio.BuildConfig.BASE_URL
import com.example.aigpsradio.data.repository.Repository
import com.example.aigpsradio.model.audio.AudioPlaybackManager
import com.example.aigpsradio.model.audio.AudioQueueManager
import com.example.aigpsradio.model.audio.QueuedTrack
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "LocationAudioViewModel"
private const val LOCATION_CHECK_INTERVAL = 30_000L // 30 sec

data class LocationAudioUiState(
    val isLoadingPlace: Boolean = false,
    val isLoadingAudio: Boolean = false,
    val errorMessage: String? = null,
    val currentPlaceName: String? = null,
    val currentTrackName: String? = null,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val queueSize: Int = 0,
    val currentTrackIndex: Int = 0,
    val hasStartedManually: Boolean = false,
    val placeImageBitmap: Bitmap? = null,
    val currentPlaceDescription: String? = null
)

/**
 * ViewModel that coordinates location tracking, place detection,
 * audio queue management, and playback.
 */
class LocationAudioViewModel(
    application: Application,
    private val repository: Repository
) : AndroidViewModel(application) {

    private val audioPlaybackManager = AudioPlaybackManager(application)
    private val queueManager = AudioQueueManager()

    private val _uiState = MutableStateFlow(LocationAudioUiState())
    val uiState: StateFlow<LocationAudioUiState> = _uiState.asStateFlow()

    private var locationCheckJob: Job? = null
    private var currentLocation: Pair<Double, Double>? = null

    private var currentPlaceCoordinates: Pair<Double, Double>? = null
    private var currentPlaceImageName: String? = null

    init {
        // Observe queue changes
        viewModelScope.launch {
            queueManager.queue.collect { queue ->
                _uiState.value = _uiState.value.copy(
                    queueSize = queue.size,
                    currentTrackIndex = queueManager.currentTrackIndex.value
                )
            }
        }

        // Observe playback state
        viewModelScope.launch {
            audioPlaybackManager.isPlaying.collect { isPlaying ->
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }
        }

        // Отслеживание состояния паузы
        viewModelScope.launch {
            audioPlaybackManager.isPaused.collect { isPaused ->
                _uiState.value = _uiState.value.copy(isPaused = isPaused)
            }
        }

        // Отслеживание имени текущего трека
        viewModelScope.launch {
            audioPlaybackManager.currentTrackName.collect { trackName ->
                _uiState.value = _uiState.value.copy(currentTrackName = trackName)
            }
        }
    }

    fun getCurrentPlaceCoordinates(): Pair<Double, Double>? {
        return currentPlaceCoordinates
    }

    /**
     * Starts periodic location checking every 30 seconds.
     */
    fun startLocationBasedPlayback(latitude: Double, longitude: Double) {
        if (locationCheckJob?.isActive == true) {
            Log.d(TAG, "Location checking already active")
            return
        }

        currentLocation = Pair(latitude, longitude)

        locationCheckJob = viewModelScope.launch {
            // Initial check
            checkLocationAndUpdatePlaylist(latitude, longitude)

            // Periodic checks every 30 seconds
            while (true) {
                delay(LOCATION_CHECK_INTERVAL)
                currentLocation?.let { (lat, lon) ->
                    checkLocationAndUpdatePlaylist(lat, lon)
                }
            }
        }

        Log.d(TAG, "Started location-based playback")
    }

    /**
     * Updates current location for the next periodic check.
     */
    fun updateLocation(latitude: Double, longitude: Double) {
        currentLocation = Pair(latitude, longitude)
    }

    /**
     * Stops periodic location checking.
     */
    fun stopLocationChecking() {
        locationCheckJob?.cancel()
        locationCheckJob = null
        Log.d(TAG, "Stopped location checking")
    }

    /**
     * Checks location, fetches nearest place, and updates playlist accordingly.
     */
    private suspend fun checkLocationAndUpdatePlaylist(latitude: Double, longitude: Double) {
        _uiState.value = _uiState.value.copy(isLoadingPlace = true, errorMessage = null)

        repository.getNearestPlace(latitude, longitude)
            .onSuccess { placeResponse ->

                currentPlaceImageName = placeResponse.image

                currentPlaceCoordinates = Pair(
                    placeResponse.latitudeResponse,
                    placeResponse.longitudeResponse
                )

                _uiState.value = _uiState.value.copy(
                    isLoadingPlace = false,
                    currentPlaceName = placeResponse.placeName,
                    currentPlaceDescription = placeResponse.description
                )

                loadPlaceImage()

                // Handle place change/continuation
                val isNewPlace = queueManager.handleNewPlace(
                    newPlaceId = placeResponse.id,
                    newPlaceName = placeResponse.placeName,
                    newAudioFiles = placeResponse.fullAudioFiles
                )

                if (isNewPlace) {
                    Log.d(TAG, "New place detected, transition will happen after current track")
                    // Текущий трек продолжает играть, очередь уже обновлена
                } else {
                    Log.d(TAG, "Same place or initial - waiting for manual start")
                }
            }
            .onFailure { error ->
                Log.e(TAG, "Failed to get nearest place: ${error.message}", error)
                _uiState.value = _uiState.value.copy(
                    isLoadingPlace = false,
                    errorMessage = "Failed to get location: ${error.localizedMessage}"
                )
            }
    }

    private fun loadPlaceImage() {
        currentPlaceImageName?.let { imageName ->
            Glide.with(getApplication<Application>().applicationContext)
                .asBitmap()
                .load("$BASE_URL/$imageName")
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        _uiState.value = _uiState.value.copy(placeImageBitmap = resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        _uiState.value = _uiState.value.copy(placeImageBitmap = null)
                    }

//                    override fun onLoadFailed(errorDrawable: Drawable?) {
//                        super.onLoadFailed(errorDrawable)
//                        _uiState.value = _uiState.value.copy(errorMessage = "Failed to load place image")
//                    }
                })
        }
    }

    /**
     * Воспроизводит следующий трек в очереди
     */
    private fun playNextInQueue() {
        val nextTrack = queueManager.getCurrentTrack()

        if (nextTrack == null) {
            Log.d(TAG, "No more tracks in queue")
            _uiState.value = _uiState.value.copy(
                isPlaying = false,
                isPaused = false,
                currentTrackName = null
            )
            return
        }

        // Скачиваем и воспроизводим трек
        downloadAndPlayTrack(nextTrack)
    }

    /**
     * Скачивает аудиофайл и запускает воспроизведение
     */
    private fun downloadAndPlayTrack(track: QueuedTrack) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAudio = true, errorMessage = null)

            repository.streamAudio(track.audioFile.audioName)
                .onSuccess { responseBody ->
                    repository.saveAudioToCache(responseBody, getApplication<Application>().cacheDir)
                        .onSuccess { file ->
                            _uiState.value = _uiState.value.copy(isLoadingAudio = false)

                            // Воспроизводим с callback завершения
                            audioPlaybackManager.play(
                                audioFile = file,
                                trackName = track.audioFile.audioName,
                                onComplete = {
                                    // Когда трек завершается, переходим к следующему
                                    onTrackCompleted(file)
                                }
                            )
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Failed to save audio: ${error.message}", error)
                            _uiState.value = _uiState.value.copy(
                                isLoadingAudio = false,
                                errorMessage = "Failed to save audio: ${error.localizedMessage}"
                            )
                            // Пробуем следующий трек при ошибке
                            handleTrackError()
                        }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to stream audio: ${error.message}", error)
                    _uiState.value = _uiState.value.copy(
                        isLoadingAudio = false,
                        errorMessage = "Failed to stream audio: ${error.localizedMessage}"
                    )
                    // Пробуем следующий трек при ошибке
                    handleTrackError()
                }
        }
    }

    /**
     * Вызывается при успешном завершении трека
     */
    private fun onTrackCompleted(audioFile: File) {
        // Удаляем временный файл
        try {
            if (audioFile.exists()) {
                audioFile.delete()
                Log.d(TAG, "Deleted temporary audio file: ${audioFile.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete audio file: ${e.message}")
        }

        // Переходим к следующему треку
        if (queueManager.moveToNext()) {
            playNextInQueue()
        } else {
            Log.d(TAG, "Playlist finished")
            _uiState.value = _uiState.value.copy(
                isPlaying = false,
                isPaused = false,
                currentTrackName = null
            )
        }
    }

    /**
     * Вызывается при ошибке воспроизведения/загрузки трека
     * Пропускает к следующему треку
     */
    private fun handleTrackError() {
        if (queueManager.moveToNext()) {
            playNextInQueue()
        } else {
            Log.d(TAG, "No more tracks after error")
        }
    }

    /**
     * Ставит воспроизведение на паузу
     */
    fun pausePlayback() {
        audioPlaybackManager.pause()
        Log.d(TAG, "Playback paused")
    }

    /**
     * Возобновляет воспроизведение после паузы
     */
    fun resumePlayback() {
        if (audioPlaybackManager.hasActivePlayer()) {
            audioPlaybackManager.resume()
            Log.d(TAG, "Playback resumed")
        } else {
            // Если нет активного плеера - запускаем первый трек
            _uiState.value = _uiState.value.copy(hasStartedManually = true)
            playNextInQueue()
            Log.d(TAG, "Playback started manually")
        }
    }

    /**
     * Полностью останавливает воспроизведение и очищает очередь
     */
    fun stopPlayback() {
        audioPlaybackManager.stop()
        queueManager.clear()
        _uiState.value = _uiState.value.copy(
            isPlaying = false,
            isPaused = false,
            currentTrackName = null,
            queueSize = 0,
            currentTrackIndex = 0
        )
        Log.d(TAG, "Playback stopped manually")
    }

    /**
     * Вручную пропускает к предыдущему треку
     */
    fun skipToPrevious() {
        audioPlaybackManager.stop()
        if (queueManager.moveToPrevious()) {
            playNextInQueue()
        }
    }

    /**
     * Вручную пропускает к следующему треку
     */
    fun skipToNext() {
        audioPlaybackManager.stop()
        if (queueManager.moveToNext()) {
            playNextInQueue()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationChecking()
        audioPlaybackManager.release()
        Log.d(TAG, "ViewModel cleared")
    }
}