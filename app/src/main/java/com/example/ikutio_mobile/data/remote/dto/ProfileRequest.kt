package com.example.ikutio_mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProfileRequest(
    @SerializedName("name")
    val name: String
)