package com.example.ikutio_mobile.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ikutio_mobile.data.remote.AuthApiService
import com.example.ikutio_mobile.data.remote.dto.TokenRefreshRequest
import com.example.ikutio_mobile.data.security.TokenManager
import com.example.ikutio_mobile.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiService: AuthApiService
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination = _startDestination.asStateFlow()

    init {
        checkUserLoginStatus()
    }

    private fun checkUserLoginStatus() {
        viewModelScope.launch {
            delay(1500)

            val accessToken = tokenManager.getAccessToken()
            val refreshToken = tokenManager.getRefreshToken()

            if (accessToken == null || refreshToken == null) {
                _startDestination.value = Screen.Login.route
                return@launch
            }

            try {
                val response = authApiService.refreshToken(TokenRefreshRequest(refreshToken))
                tokenManager.saveAccessToken(response.jwt)
                tokenManager.saveRefreshToken(response.refreshToken)
                _startDestination.value = Screen.Main.route
            } catch (e: Exception) {
                tokenManager.clearTokens()
                _startDestination.value = Screen.Login.route
            }
        }
    }
}