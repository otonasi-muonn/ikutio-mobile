package com.example.ikutio_mobile.data.remote

import com.example.ikutio_mobile.data.remote.dto.ProfileRequest
import com.example.ikutio_mobile.data.remote.dto.ProfileResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ProfileApiService {
    @POST("/profile")
    suspend fun setProfile(
        @Header("Authorization") token: String,
        @Body request: ProfileRequest
    ): ProfileResponse
}