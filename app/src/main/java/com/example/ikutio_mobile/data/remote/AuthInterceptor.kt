package com.example.ikutio_mobile.data.remote

import com.example.ikutio_mobile.data.security.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getAccessToken()
        val request = chain.request()

        return if (token != null) {
            // トークンがあれば、ヘッダーを追加してリクエストを続行
            val newRequest = request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        } else {
            // トークンがなければ、そのままリクエストを続行
            chain.proceed(request)
        }
    }
}