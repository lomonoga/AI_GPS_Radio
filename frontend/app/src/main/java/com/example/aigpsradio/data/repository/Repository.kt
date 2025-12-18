package com.example.aigpsradio.data.repository

import android.util.Log
import com.example.aigpsradio.model.remote.NearestPlaceResponse
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
     * Получает информацию о ближайшем месте по координатам.
     */
    suspend fun getNearestPlace(
        latitude: Double,
        longitude: Double,
        interests: List<String>
    ): Result<NearestPlaceResponse> = withContext(Dispatchers.IO) {
        try {
            //val interests = listOf("architecture")

            Log.d(
                "API_DEBUG",
                "lat=$latitude lon=$longitude interests=$interests"
            )
            val response = api.getNearestPlace(latitude, longitude, radius = 25000, interests)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                // Извлекаем данные из wrapper-объекта
                val placeData = body.data
                Log.d("API_DEBUG", "Nearest place request successful, place: ${placeData.placeName}")
                Result.success(placeData)
            } else {
                val errorMsg = "Server error: ${response.code()}"
                Log.e("API_DEBUG", errorMsg)
                Result.failure(Exception(errorMsg))
            }

        } catch (ce: CancellationException) {
            Log.w(TAG, "getNearestPlace cancelled", ce)
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "getNearestPlace error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Запрашивает аудио у сервера асинхронно и возвращает Result с ResponseBody или ошибкой.
     * Важно: ResponseBody нужно закрыть/потребить (например, через saveAudioToCache), чтобы не утекли ресурсы.
     */
    suspend fun streamAudio(s3key: String): Result<ResponseBody> = withContext(Dispatchers.IO) {
        try {
            val response = api.streamAudio(s3key)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                Log.d(TAG, "Stream request successful, response code = ${response.code()}")
                Result.success(body)
            } else {
                val errorMsg = "Server error: ${response.code()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
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
     * Сохраняет аудио в кэш асинхронно, возвращает успешно созданный локальный файл или ошибку.
     */
    suspend fun saveAudioToCache(
        responseBody: ResponseBody,
        cacheDir: File,
        fileName: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Используем переданное имя файла или генерируем новое
            val finalFileName = fileName ?: "streamed_audio_${System.currentTimeMillis()}.mp3"
            val audioFile = File(cacheDir, finalFileName)

            audioFile.outputStream().use { output ->
                responseBody.byteStream().use { input ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "Audio saved to cache: ${audioFile.absolutePath}, size: ${audioFile.length()} bytes")
            Result.success(audioFile)

        } catch (ce: CancellationException) {
            Log.w(TAG, "saveAudioToCache cancelled", ce)
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "saveAudioToCache error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Вспомогательная функция для загрузки и сохранения аудио одной операцией.
     */
    suspend fun downloadAndCacheAudio(
        s3Key: String,
        cacheDir: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Загружаем аудио
            val streamResult = streamAudio(s3Key)

            if (streamResult.isFailure) {
                return@withContext Result.failure(
                    streamResult.exceptionOrNull() ?: Exception("Failed to stream audio")
                )
            }

            val responseBody = streamResult.getOrNull()
                ?: return@withContext Result.failure(Exception("Empty response body"))

            // Сохраняем в кэш
            saveAudioToCache(responseBody, cacheDir, s3Key.substringAfterLast('/'))

        } catch (ce: CancellationException) {
            Log.w(TAG, "downloadAndCacheAudio cancelled", ce)
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "downloadAndCacheAudio error: ${e.message}", e)
            Result.failure(e)
        }
    }
}