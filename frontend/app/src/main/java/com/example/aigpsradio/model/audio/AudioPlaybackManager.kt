package com.example.aigpsradio.model.audio

import android.app.Application
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

private const val TAG = "AudioPlaybackManager"

/**
 * Управляет воспроизведением звука с помощью MediaPlayer.
 * Поддерживает паузу/возобновление, обработку завершения треков.
 */
class AudioPlaybackManager(application: Application) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentFile: File? = null
    private var onCompletionCallback: (() -> Unit)? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _currentTrackName = MutableStateFlow<String?>(null)
    val currentTrackName: StateFlow<String?> = _currentTrackName.asStateFlow()

    /**
     * Воспроизводит аудиофайл и вызывает метод onComplete по завершении
     */
    fun play(audioFile: File, trackName: String, onComplete: () -> Unit) {
        try {
            stop() // Останавливаем предыдущий трек

            currentFile = audioFile
            onCompletionCallback = onComplete

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)

                setOnCompletionListener {
                    Log.d(TAG, "Track completed: $trackName")
                    _isPlaying.value = false
                    _isPaused.value = false
                    _currentTrackName.value = null
                    onComplete()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    _isPaused.value = false
                    _currentTrackName.value = null
                    false
                }

                prepare() // Синхронная подготовка
                start()

                _isPlaying.value = true
                _isPaused.value = false
                _currentTrackName.value = trackName
                Log.d(TAG, "Started playing: $trackName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.message}", e)
            _isPlaying.value = false
            _isPaused.value = false
            _currentTrackName.value = null
        }
    }

    /**
     * Ставит воспроизведение на паузу
     */
    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                _isPaused.value = true
                Log.d(TAG, "Playback paused")
            }
        }
    }

    /**
     * Возобновляет воспроизведение после паузы
     */
    fun resume() {
        mediaPlayer?.let { player ->
            if (_isPaused.value) {
                player.start()
                _isPlaying.value = true
                _isPaused.value = false
                Log.d(TAG, "Playback resumed")
            }
        }
    }

    /**
     * Полностью останавливает воспроизведение и освобождает ресурсы
     */
    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        currentFile = null
        onCompletionCallback = null
        _isPlaying.value = false
        _isPaused.value = false
        _currentTrackName.value = null
        Log.d(TAG, "Playback stopped")
    }

    /**
     * Возвращает текущую позицию воспроизведения в мс
     */
    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    /**
     * Возвращает общую длительность в мс
     */
    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    /**
     * Проверяет, есть ли активный MediaPlayer
     */
    fun hasActivePlayer(): Boolean {
        return mediaPlayer != null
    }

    fun release() {
        stop()
    }
}