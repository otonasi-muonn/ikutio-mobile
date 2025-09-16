package com.example.ikutio_mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StreetViewMetadataResponse(
    @SerializedName("status")
    val status: String, // "OK" または "ZERO_RESULTS"

    @SerializedName("location")
    val location: LocationLatLng?
)

data class LocationLatLng(
    @SerializedName("lat")
    val lat: Double,

    @SerializedName("lng")
    val lng: Double
)