package com.example.aigpsradio.model.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

// API Interface

interface SimpleApi {

    @GET("api/poi/nearby")
    suspend fun getNearestPlace(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Int,
        @Query("interests") interests: List<String>,
    ): Response<NearestPlaceApiResponse>

    @Streaming
    @GET("/s3/files/{path}")
    suspend fun streamAudio(
        @Path("path", encoded = true) filePath: String
    ): Response<ResponseBody>
}
