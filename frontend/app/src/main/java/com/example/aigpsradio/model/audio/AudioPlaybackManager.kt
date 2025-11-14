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
 * Обрабатывает callback воспроизведения, остановки и завершения.
 */

class AudioPlaybackManager(application: Application) {

    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrackName = MutableStateFlow<String?>(null)  // audioName?
    val currentTrackName: StateFlow<String?> = _currentTrackName.asStateFlow()


    // Воспроизводит аудиофайл и вызывает метод onComplete по завершении

    fun play(audioFile: File, trackName: String, onComplete: () -> Unit) {
        try {
            stop() // Старый MediaPlayer точно остановлен и освобождён перед созданием нового

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)

                setOnCompletionListener {  // регистрируется слушатель окончания трека
                    Log.d(TAG, "Track completed: $trackName")
                    _isPlaying.value = false
                    _currentTrackName.value = null
                    onComplete()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    _currentTrackName.value = null
                    false
                }
                prepare() // синхронная блокирующая операция на UI потоке (подготовка к воспроизведению)
                start()

                _isPlaying.value = true
                _currentTrackName.value = trackName
                Log.d(TAG, "Started playing: $trackName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.message}", e)
            _isPlaying.value = false
            _currentTrackName.value = null
        }
    }

    // Stop current playback and clean/release MediaPlayer.
    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentTrackName.value = null
        Log.d(TAG, "Playback stopped")
    }

    // Возвращает текущую позицию воспроизведения в мс
    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    // Возвращает общую длительность в мс
    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    fun release() {
        stop()
    }
}