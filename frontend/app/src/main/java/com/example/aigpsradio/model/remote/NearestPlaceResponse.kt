package com.example.aigpsradio.model.remote

import com.google.gson.annotations.SerializedName

// Response Models

data class NearestPlaceApiResponse(
    @SerializedName("data") val data: NearestPlaceResponse
)

data class NearestPlaceResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val placeName: String,
    @SerializedName("description") val description: String,
    @SerializedName("latitude") val latitudeResponse: Double,
    @SerializedName("longitude") val longitudeResponse: Double,
    @SerializedName("image_file") val imageFile: ImageFile,
    @SerializedName("full_audio_files") val fullAudioFiles: List<AudioFile>,
    @SerializedName("short_audio_file") val shortAudioFile: AudioFile,
    @SerializedName("interests") val interests: List<String>,
    @SerializedName("created_at") val createdAt: String
)