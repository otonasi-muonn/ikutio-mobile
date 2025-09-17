package com.example.ikutio_mobile.data.remote

import com.example.ikutio_mobile.data.remote.dto.PathDataRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GameApiService {
    @POST("/post_locations")
    suspend fun sendPathData(@Body request: PathDataRequest): Response<Unit>
}