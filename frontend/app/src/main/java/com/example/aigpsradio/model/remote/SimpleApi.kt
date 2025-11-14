package com.example.aigpsradio.model.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

// API Interface

interface SimpleApi {

    @POST("nearest")
    suspend fun getNearestPlace(
        @Body payload: LocationPayload
    ): Response<NearestPlaceResponse>

    @Streaming
    @POST("stream")
    suspend fun streamAudio(
        @Body request: AudioRequest
    ): Response<ResponseBody>
}