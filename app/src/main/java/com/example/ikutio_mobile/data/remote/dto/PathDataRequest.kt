package com.example.ikutio_mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PathDataRequest(
    @SerializedName("pathData")
    val pathData: List<PathDataItem>
)