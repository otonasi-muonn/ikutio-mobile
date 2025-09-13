package com.example.ikutio_mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TokenRefreshRequest(
    @SerializedName("refreshtoken")
    val refreshToken: String
)