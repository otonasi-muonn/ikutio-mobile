package com.example.ikutio_mobile.ui.main

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ikutio_mobile.data.repository.LocationRepository
import com.example.ikutio_mobile.services.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val INITIAL_TIME = "00:00:00"

data class MainUiState(
    val isServiceRunning: Boolean = false,
    val statusText: String = "待機中",
    val elapsedTimeText: String = INITIAL_TIME
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun startTimerAndUpdateState() {
        _uiState.value = _uiState.value.copy(isServiceRunning = true, statusText = "記録中")
        startTimer()
    }

    fun stopTimerAndUpdateState() {
        // UIの状態を先に「送信中」に更新
        _uiState.value = _uiState.value.copy(statusText = "データを送信中...")
        stopTimer()

        // データ転送処理を非同期で実行
        viewModelScope.launch {
            try {
                // Repositoryのデータ転送関数を呼び出す
                locationRepository.sendPathDataToServer()

                // 成功したらUIの状態を「待機中」に戻す
                _uiState.value = _uiState.value.copy(
                    isServiceRunning = false,
                    statusText = "待機中",
                    elapsedTimeText = INITIAL_TIME
                )
            } catch (e: Exception) {
                // 失敗したらUIにエラーを通知
                _uiState.value = _uiState.value.copy(
                    isServiceRunning = false, // サービス自体は止まっているのでfalseに
                    statusText = "送信失敗",
                    elapsedTimeText = INITIAL_TIME
                )
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                val elapsedTimeInSeconds = (System.currentTimeMillis() - startTime) / 1000
                _uiState.value = _uiState.value.copy(
                    elapsedTimeText = formatTime(elapsedTimeInSeconds)
                )
                delay(1000L)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }
}