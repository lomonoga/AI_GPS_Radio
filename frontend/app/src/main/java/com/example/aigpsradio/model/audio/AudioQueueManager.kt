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
 * Represents a pending place transition.
 */
data class PendingPlaceTransition(
    val placeId: Int,
    val placeName: String,
    val audioFiles: List<AudioFile>
)

class AudioQueueManager {

    private val _queue = MutableStateFlow<List<QueuedTrack>>(emptyList())
    val queue: StateFlow<List<QueuedTrack>> = _queue.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

    private val _currentPlaceId = MutableStateFlow<Int?>(null)
    val currentPlaceId: StateFlow<Int?> = _currentPlaceId.asStateFlow()

    // NEW: Pending transition that will be applied after current track finishes
    private val _pendingTransition = MutableStateFlow<PendingPlaceTransition?>(null)
    val pendingTransition: StateFlow<PendingPlaceTransition?> = _pendingTransition.asStateFlow()

    fun getCurrentTrack(): QueuedTrack? {
        val index = _currentTrackIndex.value
        val queueList = _queue.value
        return if (index in queueList.indices) queueList[index] else null
    }

    fun moveToPrevious(): Boolean {
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) {
            Log.d(TAG, "Queue is empty, cannot move to previous")
            return false
        }

        val currentIndex = _currentTrackIndex.value

        // Clamp index to valid range first
        val clampedIndex = currentIndex.coerceIn(0, currentQueue.size - 1)

        if (clampedIndex > 0) {
            _currentTrackIndex.value = clampedIndex - 1
            Log.d(TAG, "Moved to previous track: index=${clampedIndex - 1}")
            return true
        }

        Log.d(TAG, "Already at first track, restarting current track")
        _currentTrackIndex.value = 0
        return true  // Return true to restart the current track
    }

    fun moveToNext(): Boolean {
        // Check if we should apply pending transition
        val pending = _pendingTransition.value
        if (pending != null) {
            Log.d(TAG, "Applying pending transition to ${pending.placeName}")
            applyPendingTransition()
            return _queue.value.isNotEmpty()
        }

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

        // No more tracks - keep index at last valid position
        _currentTrackIndex.value = currentQueue.size - 1  // Changed from currentQueue.size
        Log.d(TAG, "No more tracks in queue")
        return false
    }

    /**
     * Handles a new place detection.
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

        // First time - initialize immediately
        if (currentId == null) {
            Log.d(TAG, "Initial place: $newPlaceName (ID: $newPlaceId)")
            initializeQueue(newPlaceId, newPlaceName, newAudioFiles)
            _currentPlaceId.value = newPlaceId
            return false
        }

        // Same place - do nothing
        if (currentId == newPlaceId) {
            Log.d(TAG, "Same place, continuing: $newPlaceName (ID: $newPlaceId)")
            return false
        }

        // NEW: Different place - queue the transition
        Log.d(TAG, "Place change detected: $currentId -> $newPlaceId ($newPlaceName). Queuing transition.")
        _pendingTransition.value = PendingPlaceTransition(
            placeId = newPlaceId,
            placeName = newPlaceName,
            audioFiles = newAudioFiles
        )
        return true
    }

    /**
     * NEW: Applies the pending place transition.
     * Called automatically in moveToNext() when current track finishes.
     */
    private fun applyPendingTransition() {
        val pending = _pendingTransition.value ?: return

        transitionToNewPlace(
            newPlaceId = pending.placeId,
            newPlaceName = pending.placeName,
            newAudioFiles = pending.audioFiles
        )

        _currentPlaceId.value = pending.placeId
        _pendingTransition.value = null

        Log.d(TAG, "Pending transition applied to ${pending.placeName}")
    }

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
        // Simply replace with new queue since we're transitioning after current track finished
        val newTracks = newAudioFiles.map { audioFile ->
            QueuedTrack(audioFile, newPlaceId, newPlaceName)
        }

        _queue.value = newTracks
        _currentTrackIndex.value = 0
        Log.d(TAG, "Transitioned to new place: ${newTracks.size} tracks for $newPlaceName")
    }

    /**
     * Clears the entire queue.
     */
    fun clear() {
        _queue.value = emptyList()
        _currentTrackIndex.value = 0
        _currentPlaceId.value = null
        _pendingTransition.value = null
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