package com.example.ikutio_mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TokenRefreshResponse(
    @SerializedName("jwt")
    val jwt: String,

    @SerializedName("refreshtoken")
    val refreshToken: String
)