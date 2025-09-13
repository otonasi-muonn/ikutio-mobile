package com.example.ikutio_mobile.data.remote

import com.example.ikutio_mobile.data.remote.dto.LoginRequest
import com.example.ikutio_mobile.data.remote.dto.LoginResponse
import com.example.ikutio_mobile.data.remote.dto.TokenRefreshRequest
import com.example.ikutio_mobile.data.remote.dto.TokenRefreshResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/refresh/auth")
    suspend fun refreshToken(@Body request: TokenRefreshRequest): TokenRefreshResponse
}