package com.example.aigpsradio.model.remote

import com.google.gson.annotations.SerializedName

// Request Model (отправляем filename, чтобы запросить аудио)

data class AudioRequest(
    @SerializedName("filename") val audioName: String,
)