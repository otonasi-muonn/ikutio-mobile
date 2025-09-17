package com.example.ikutio_mobile.data.remote

import com.example.ikutio_mobile.BuildConfig
import com.example.ikutio_mobile.data.remote.dto.StreetViewMetadataResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MapsApiService {
    @GET("https://maps.googleapis.com/maps/api/streetview/metadata")
    suspend fun getStreetViewMetadata(
        @Query("location") location: String,
        @Query("key") key: String = BuildConfig.MAPS_API_KEY
    ): StreetViewMetadataResponse
}