package com.example.ikutio_mobile.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ikutio_mobile.data.repository.LocationRepository
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
private const val INITIAL_RAW_LOCATION_TEXT = "生の座標: ---"
private const val INITIAL_NORMALIZED_LOCATION_TEXT = "正規化座標: ---"

data class MainUiState(
    val isServiceRunning: Boolean = false,
    val statusText: String = "待機中",
    val elapsedTimeText: String = INITIAL_TIME,
    val rawLocationText: String = INITIAL_RAW_LOCATION_TEXT,
    val normalizedLocationText: String = INITIAL_NORMALIZED_LOCATION_TEXT
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            locationRepository.latestRawLocation.collect { loc ->
                _uiState.value = _uiState.value.copy(
                    rawLocationText = if (loc != null) "生の座標: ${String.format(Locale.US, "%.6f, %.6f", loc.first, loc.second)}" else INITIAL_RAW_LOCATION_TEXT
                )
            }
        }
        viewModelScope.launch {
            locationRepository.latestNormalizedLocation.collect { loc ->
                _uiState.value = _uiState.value.copy(
                    normalizedLocationText = if (loc != null) "正規化座標: ${String.format(Locale.US, "%.6f, %.6f", loc.first, loc.second)}" else "正規化座標: (なし)" // 「なし」の方が適切かもしれません
                )
            }
        }
    }

    fun startTimerAndUpdateState() {
        _uiState.value = _uiState.value.copy(
            isServiceRunning = true,
            statusText = "記録中"
            // rawLocationText と normalizedLocationText は Flow の収集によって自動的に更新されるため、ここでは初期化しない
        )
        startTimer()
    }

    fun stopTimerAndUpdateState() {
        _uiState.value = _uiState.value.copy(statusText = "データを送信中...")
        stopTimer()

        viewModelScope.launch {
            try {
                locationRepository.sendPathDataToServer()
                _uiState.value = _uiState.value.copy(
                    isServiceRunning = false,
                    statusText = "待機中",
                    elapsedTimeText = INITIAL_TIME,
                    rawLocationText = INITIAL_RAW_LOCATION_TEXT, // サービス停止時に初期化
                    normalizedLocationText = INITIAL_NORMALIZED_LOCATION_TEXT // サービス停止時に初期化
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isServiceRunning = false,
                    statusText = "送信失敗: ${e.message}", // エラーメッセージをUIに含める
                    elapsedTimeText = INITIAL_TIME,
                    rawLocationText = INITIAL_RAW_LOCATION_TEXT, // エラー時も初期化
                    normalizedLocationText = INITIAL_NORMALIZED_LOCATION_TEXT // エラー時も初期化
                )
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            // サービス開始時に経過時間をリセット
            _uiState.value = _uiState.value.copy(elapsedTimeText = formatTime(0))
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