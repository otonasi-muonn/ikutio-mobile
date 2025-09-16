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

// UIの状態を表すデータクラス
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
    private val tokenManager: TokenManager // TokenManagerを注入
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    // UIからのイベントで呼び出される関数
    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun login() {
        // すでにローディング中は何もしない
        if (_uiState.value.isLoading) return

        // viewModelScopeを使ってコルーチンを開始（非同期処理）
        viewModelScope.launch {
            // ① ローディング状態を開始
            _uiState.value = _uiState.value.copy(isLoading = true, loginError = null)

            try {
                val request = LoginRequest(
                    email = _uiState.value.email,
                    password = _uiState.value.password
                )
                // ② APIを呼び出し
                val response = authApiService.login(request)

                tokenManager.saveTokens(
                    accessToken = response.jwt,
                    refreshToken = response.refreshToken
                )
                _uiState.value = _uiState.value.copy(isLoading = false, loginSuccess = true)

            } catch (e: Exception) {
                // ④ エラー状態をUIに通知
                _uiState.value = _uiState.value.copy(isLoading = false, loginError = "ログインに失敗しました: ${e.message}")
            }
        }
    }
}