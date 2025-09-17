package com.example.ikutio_mobile.data.remote

import com.example.ikutio_mobile.BuildConfig
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

data class SnapToRoadsResponse(
    @SerializedName("snappedPoints")
    val snappedPoints: List<SnappedPoint>?
)

data class SnappedPoint(
    @SerializedName("location")
    val location: RoadLocation?,
    @SerializedName("originalIndex")
    val originalIndex: Int?,
    @SerializedName("placeId")
    val placeId: String?
)

data class RoadLocation(
    @SerializedName("latitude")
    val latitude: Double?,
    @SerializedName("longitude")
    val longitude: Double?
)

interface MapsApiService {

    @GET("https://roads.googleapis.com/v1/snapToRoads")
    suspend fun snapToRoads(
        @Query("path") path: String, 
        @Query("interpolate") interpolate: Boolean = false, 
        @Query("key") key: String = BuildConfig.MAPS_API_KEY
    ): SnapToRoadsResponse
}