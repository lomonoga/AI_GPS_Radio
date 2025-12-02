package com.example.aigpsradio.model.remote

import com.google.gson.annotations.SerializedName

// Response Models

data class ImageFile(
    @SerializedName("id") val id: Int,
    @SerializedName("s3_key") val s3Key: String,
    @SerializedName("file_name") val audioName: String,
    @SerializedName("file_size") val fileSize: Long,
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("serial_number") val serialNumber: Int,
    @SerializedName("is_short") val isShort: Boolean,
    @SerializedName("created_at") val createdAt: String
)
