package com.example.ikutio_mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PathDataItem(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("timestamp")
    val timestamp: String // サーバーの仕様に合わせてISO 8601形式の文字列で送信
)