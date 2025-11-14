package com.example.aigpsradio.model.audio

import android.util.Log
import com.example.aigpsradio.model.remote.AudioFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "AudioQueueManager"

/**
 * Represents a track in the queue with its place ID.
 */
data class QueuedTrack(
    val audioFile: AudioFile,
    val placeId: Int,
    val placeName: String
)

/**
 * Manages the audio queue and handles place transitions.
 */
class AudioQueueManager {

    private val _queue = MutableStateFlow<List<QueuedTrack>>(emptyList())
    val queue: StateFlow<List<QueuedTrack>> = _queue.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

    private val _currentPlaceId = MutableStateFlow<Int?>(null)
    val currentPlaceId: StateFlow<Int?> = _currentPlaceId.asStateFlow()

    /**
     * Returns the current track if available.
     */
    fun getCurrentTrack(): QueuedTrack? {
        val index = _currentTrackIndex.value
        val queueList = _queue.value
        return if (index in queueList.indices) queueList[index] else null
    }

    /**
     * Moves to the next track in queue.
     * Returns true if there's a next track, false if queue is empty.
     */
    fun moveToNext(): Boolean {
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) {
            _currentTrackIndex.value = 0
            return false
        }

        val nextIndex = _currentTrackIndex.value + 1
        if (nextIndex < currentQueue.size) {
            _currentTrackIndex.value = nextIndex
            Log.d(TAG, "Moved to next track: index=$nextIndex")
            return true
        }

        // No more tracks
        _currentTrackIndex.value = currentQueue.size
        Log.d(TAG, "No more tracks in queue")
        return false
    }

    /**
     * Handles a new place: either continues current queue or transitions to new place.
     *
     * @param newPlaceId The ID of the newly detected place
     * @param newPlaceName The name of the new place
     * @param newAudioFiles The audio files for the new place
     * @return true if this is a new place (transition needed), false if same place
     */
    fun handleNewPlace(
        newPlaceId: Int,
        newPlaceName: String,
        newAudioFiles: List<AudioFile>
    ): Boolean {
        val currentId = _currentPlaceId.value

        // First time or same place - just update/continue
        if (currentId == null || currentId == newPlaceId) {
            if (currentId == null) {
                Log.d(TAG, "Initial place: $newPlaceName (ID: $newPlaceId)")
                initializeQueue(newPlaceId, newPlaceName, newAudioFiles)
            } else {
                Log.d(TAG, "Same place, continuing: $newPlaceName (ID: $newPlaceId)")
            }
            _currentPlaceId.value = newPlaceId
            return false
        }

        // New place detected - need transition
        Log.d(TAG, "Place changed: $currentId -> $newPlaceId ($newPlaceName)")
        transitionToNewPlace(newPlaceId, newPlaceName, newAudioFiles)
        _currentPlaceId.value = newPlaceId
        return true
    }

    /**
     * Initializes the queue with audio files from a place.
     */
    private fun initializeQueue(placeId: Int, placeName: String, audioFiles: List<AudioFile>) {
        val newQueue = audioFiles.map { audioFile ->
            QueuedTrack(audioFile, placeId, placeName)
        }
        _queue.value = newQueue
        _currentTrackIndex.value = 0
        Log.d(TAG, "Queue initialized with ${newQueue.size} tracks for $placeName")
    }

    /**
     * Transitions to a new place by removing old tracks (except currently playing)
     * and adding new place's tracks.
     */
    private fun transitionToNewPlace(
        newPlaceId: Int,
        newPlaceName: String,
        newAudioFiles: List<AudioFile>
    ) {
        val currentQueue = _queue.value.toMutableList()
        val currentIndex = _currentTrackIndex.value

        // Create new tracks for the new place
        val newTracks = newAudioFiles.map { audioFile ->
            QueuedTrack(audioFile, newPlaceId, newPlaceName)
        }

        // If there's a currently playing track, keep it and remove all after it
        if (currentIndex < currentQueue.size) {
            // Keep tracks up to and including current
            val tracksToKeep = currentQueue.subList(0, currentIndex + 1)

            // Add new place tracks after current track
            val updatedQueue = tracksToKeep + newTracks

            _queue.value = updatedQueue
            Log.d(TAG, "Transitioned: kept current track, removed ${currentQueue.size - currentIndex - 1} old tracks, added ${newTracks.size} new tracks")
        } else {
            // No current track playing, just set new queue
            _queue.value = newTracks
            _currentTrackIndex.value = 0
            Log.d(TAG, "Transitioned: no current track, set new queue with ${newTracks.size} tracks")
        }
    }

    /**
     * Clears the entire queue.
     */
    fun clear() {
        _queue.value = emptyList()
        _currentTrackIndex.value = 0
        _currentPlaceId.value = null
        Log.d(TAG, "Queue cleared")
    }

    /**
     * Returns remaining tracks count (including current).
     */
    fun getRemainingTracksCount(): Int {
        val currentQueue = _queue.value
        val currentIndex = _currentTrackIndex.value
        return maxOf(0, currentQueue.size - currentIndex)
    }
}