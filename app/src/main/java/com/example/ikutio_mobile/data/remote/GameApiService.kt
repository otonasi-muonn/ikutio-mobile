package com.example.ikutio_mobile.data.remote

import com.example.ikutio_mobile.data.remote.dto.PathDataRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GameApiService {
    @POST("/api/v1/games/paths")
    suspend fun sendPathData(@Body request: PathDataRequest): Response<Unit> // レスポンスボディがない場合はUnit
}