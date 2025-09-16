package com.example.ikutio_mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("jwt")
    val jwt: String,

    @SerializedName("refresh_token")
    val refreshToken: String
)