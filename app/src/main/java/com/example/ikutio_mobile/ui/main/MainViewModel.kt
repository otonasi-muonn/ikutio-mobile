package com.example.ikutio_mobile.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

// 指摘3を反映：初期値を定数化
private const val INITIAL_TIME = "00:00:00"

data class MainUiState(
    val isServiceRunning: Boolean = false,
    val statusText: String = "待機中",
    val elapsedTimeText: String = INITIAL_TIME
)

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // 指摘1を反映：Contextへの依存をなくし、状態更新とタイマー開始の責務に集中
    fun startTimerAndUpdateState() {
        _uiState.value = _uiState.value.copy(isServiceRunning = true, statusText = "記録中")
        startTimer()
    }

    // 指摘1を反映：Contextへの依存をなくし、状態更新とタイマー停止の責務に集中
    fun stopTimerAndUpdateState() {
        _uiState.value = _uiState.value.copy(
            isServiceRunning = false,
            statusText = "待機中",
            elapsedTimeText = INITIAL_TIME
        )
        stopTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            // 指摘2を反映：while(true)からwhile(isActive)に変更
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