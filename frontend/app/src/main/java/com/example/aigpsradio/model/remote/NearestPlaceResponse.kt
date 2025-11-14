package com.example.aigpsradio.model.remote

import com.google.gson.annotations.SerializedName

// Response Models

data class NearestPlaceResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val placeName: String,
    @SerializedName("description") val description: String,
    @SerializedName("latitude") val latitudeResponse: Double,
    @SerializedName("longitude") val longitudeResponse: Double,
    @SerializedName("full_audio_files") val fullAudioFiles: List<AudioFile>,
    @SerializedName("short_audio_file") val shortAudioFile: AudioFile,  // пока не используется
    @SerializedName("created_at") val createdAt: String,
)
