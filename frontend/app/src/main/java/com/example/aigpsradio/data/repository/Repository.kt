package com.example.aigpsradio.data.repository

import android.util.Log
import com.example.aigpsradio.model.remote.AudioRequest
import com.example.aigpsradio.model.remote.SimpleApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File

private const val TAG = "AudioRepository"

class Repository(
    private val api: SimpleApi
) {
    /**
     * Запрашивает аудио у сервера асинхронно и возвращает Result с ResponseBody или ошибкой.
     * Важно: ResponseBody нужно закрыть/потребить (например, через saveAudioToCache), чтобы не утекли ресурсы.
     */

    suspend fun streamAudio(audioName: String): Result<ResponseBody> = withContext(Dispatchers.IO) {

        try {
            val response = api.streamAudio(AudioRequest(audioName))
            val body = response.body()

            if (response.isSuccessful && body != null) {
                Log.d(TAG, "Stream request successful,response code = ${response.code()}")
                Result.success(body)
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }

        } catch (ce: CancellationException) {
            Log.w(TAG, "streamAudio cancelled", ce)
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "streamAudio error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Сохраняет аудио в кэш ассинхронно, возвращает успешно созданный локальный файл или ошибку.
     */

    suspend fun saveAudioToCache(
        responseBody: ResponseBody,
        cacheDir: File
    ): Result<File> = withContext(Dispatchers.IO) {

        try {
            val audioFile = File(cacheDir, "streamed_audio_${System.currentTimeMillis()}.mp3")
            // никогда не переиспользуем ранее скачанный файл — даже если это тот же трек!
            // присваиваем ему имя в мс

            audioFile.outputStream().use { output ->
                responseBody.byteStream().use { input ->
                    input.copyTo(output)
                }
            }
            Result.success(audioFile)

        } catch (ce: CancellationException) {
            Log.w(TAG, "saveAudioToCache cancelled", ce)
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "saveAudioToCache error: ${e.message}", e)
            Result.failure(e)
        }
    }
}