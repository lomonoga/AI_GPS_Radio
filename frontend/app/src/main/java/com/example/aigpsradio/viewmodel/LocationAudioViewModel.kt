package com.example.aigpsradio.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    val queueSize: Int = 0,
    val currentTrackIndex: Int = 0
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

        // Observe current track name
        viewModelScope.launch {
            audioPlaybackManager.currentTrackName.collect { trackName ->
                _uiState.value = _uiState.value.copy(currentTrackName = trackName)
            }
        }
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
                _uiState.value = _uiState.value.copy(
                    isLoadingPlace = false,
                    currentPlaceName = placeResponse.placeName
                )

                // Handle place change/continuation
                val isNewPlace = queueManager.handleNewPlace(
                    newPlaceId = placeResponse.id,
                    newPlaceName = placeResponse.placeName,
                    newAudioFiles = placeResponse.fullAudioFiles
                )

                if (isNewPlace) {
                    Log.d(TAG, "New place detected, transition will happen after current track")
                    // Current track continues playing, queue is already updated
                } else {
                    // Same place or initial - start playback if not playing
                    if (!audioPlaybackManager.isPlaying.value) {
                        playNextInQueue()
                    }
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

    /**
     * Plays the next track in the queue.
     */
    private fun playNextInQueue() {
        val nextTrack = queueManager.getCurrentTrack()

        if (nextTrack == null) {
            Log.d(TAG, "No more tracks in queue")
            _uiState.value = _uiState.value.copy(
                isPlaying = false,
                currentTrackName = null
            )
            return
        }

        // Download and play the track
        downloadAndPlayTrack(nextTrack)
    }

    /**
     * Downloads audio file and starts playback.
     */
    private fun downloadAndPlayTrack(track: QueuedTrack) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAudio = true, errorMessage = null)

            repository.streamAudio(track.audioFile.audioName)
                .onSuccess { responseBody ->
                    repository.saveAudioToCache(responseBody, getApplication<Application>().cacheDir)
                        .onSuccess { file ->
                            _uiState.value = _uiState.value.copy(isLoadingAudio = false)

                            // Play with completion callback
                            audioPlaybackManager.play(
                                audioFile = file,
                                trackName = track.audioFile.audioName,
                                onComplete = {
                                    // When track completes, move to next
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
                            // Try next track on error
                            handleTrackError()
                        }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to stream audio: ${error.message}", error)
                    _uiState.value = _uiState.value.copy(
                        isLoadingAudio = false,
                        errorMessage = "Failed to stream audio: ${error.localizedMessage}"
                    )
                    // Try next track on error
                    handleTrackError()
                }
        }
    }

    /**
     * Called when a track finishes playing successfully.
     */
    private fun onTrackCompleted(audioFile: File) {
        // Clean up the temporary file
        try {
            if (audioFile.exists()) {
                audioFile.delete()
                Log.d(TAG, "Deleted temporary audio file: ${audioFile.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete audio file: ${e.message}")
        }

        // Move to next track
        if (queueManager.moveToNext()) {
            playNextInQueue()
        } else {
            Log.d(TAG, "Playlist finished")
            _uiState.value = _uiState.value.copy(
                isPlaying = false,
                currentTrackName = null
            )
        }
    }

    /**
     * Called when there's an error playing/downloading a track.
     * Skips to next track.
     */
    private fun handleTrackError() {
        if (queueManager.moveToNext()) {
            playNextInQueue()
        } else {
            Log.d(TAG, "No more tracks after error")
        }
    }

    /**
     * Manually stops playback and clears queue.
     */
    fun stopPlayback() {
        audioPlaybackManager.stop()
        queueManager.clear()
        _uiState.value = _uiState.value.copy(
            isPlaying = false,
            currentTrackName = null,
            queueSize = 0,
            currentTrackIndex = 0
        )
        Log.d(TAG, "Playback stopped manually")
    }

    /**
     * Skips to next track manually.
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