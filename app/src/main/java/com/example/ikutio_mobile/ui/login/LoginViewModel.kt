package com.example.ikutio_mobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ikutio_mobile.data.remote.AuthApiService
import com.example.ikutio_mobile.data.remote.dto.LoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.ikutio_mobile.data.security.TokenManager

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val loginError: String? = null,
    val loginSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun login() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loginError = null)

            try {
                val request = LoginRequest(
                    email = _uiState.value.email,
                    password = _uiState.value.password
                )
                val response = authApiService.login(request)

                tokenManager.saveAccessToken(response.jwt)
                tokenManager.saveRefreshToken(response.refreshToken)

                _uiState.value = _uiState.value.copy(isLoading = false, loginSuccess = true)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, loginError = "ログインに失敗しました: ${e.message}")
            }
        }
    }
}