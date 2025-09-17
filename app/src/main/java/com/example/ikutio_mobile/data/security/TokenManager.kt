package com.example.ikutio_mobile.data.security

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val prefs: SharedPreferences
) {
    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }

    // アクセストークンだけを保存する
    fun saveAccessToken(token: String) {
        prefs.edit { putString(KEY_ACCESS_TOKEN, token) }
    }

    // リフレッシュトークンだけを保存する
    fun saveRefreshToken(token: String) {
        prefs.edit { putString(KEY_REFRESH_TOKEN, token) }
    }

    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    fun clearTokens() {
        prefs.edit {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
        }
    }
}