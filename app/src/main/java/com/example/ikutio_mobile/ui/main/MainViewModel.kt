package com.example.ikutio_mobile.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ikutio_mobile.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val INITIAL_TIME = "00:00:00"
private const val INITIAL_DISTANCE_TEXT = "総移動距離: ---"

data class MainUiState(
    val isServiceRunning: Boolean = false,
    val statusText: String = "待機中",
    val elapsedTimeText: String = INITIAL_TIME,
    val rawLocationText: String = "生の座標: ---",
    val normalizedLocationText: String = "正規化座標: ---",
    val totalDistanceText: String = INITIAL_DISTANCE_TEXT
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        locationRepository.latestRawLocation
            .onEach { locationPair ->
                // ★ 表示精度を %.8f に変更
                val text = locationPair?.let { (lat, lng) -> "生の座標: ${String.format(Locale.US, "%.8f, %.8f", lat, lng)}" } ?: "生の座標: ---"
                _uiState.update { it.copy(rawLocationText = text) }
            }.launchIn(viewModelScope)

        locationRepository.latestNormalizedLocation
            .onEach { locationPair ->
                val text = locationPair?.let { (lat, lng) -> "正規化座標: ${String.format(Locale.US, "%.6f, %.6f", lat, lng)}" } ?: "正規化座標: (なし)"
                _uiState.update { it.copy(normalizedLocationText = text) }

                if (locationPair != null) {
                    updateTotalDistanceDisplay()
                }
            }.launchIn(viewModelScope)

        updateTotalDistanceDisplay()
    }

    fun startTimerAndUpdateState() {
        _uiState.update { it.copy(isServiceRunning = true, statusText = "記録中") }
        startTimer()
    }

    fun stopTimerAndUpdateState() {
        _uiState.update { it.copy(statusText = "データを送信中...") }
        stopTimer()
        viewModelScope.launch {
            try {
                locationRepository.sendPathDataToServer()
                _uiState.update {
                    it.copy(
                        isServiceRunning = false,
                        statusText = "待機中",
                        elapsedTimeText = INITIAL_TIME,
                        rawLocationText = "生の座標: ---",
                        normalizedLocationText = "正規化座標: ---"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isServiceRunning = false,
                        statusText = "送信失敗",
                        elapsedTimeText = INITIAL_TIME,
                        rawLocationText = "生の座標: ---",
                        normalizedLocationText = "正規化座標: ---"
                    )
                }
            } finally {
                updateTotalDistanceDisplay()
            }
        }
    }

    private fun updateTotalDistanceDisplay() {
        viewModelScope.launch {
            try {
                val distanceMeters = locationRepository.getTotalDistanceTraveled()
                _uiState.update { it.copy(totalDistanceText = formatDistanceForUi(distanceMeters)) }
            } catch (e: Exception) {
                // Log.e("MainViewModel", "Failed to get total distance", e)
                _uiState.update { it.copy(totalDistanceText = "総移動距離: 計算エラー") }
            }
        }
    }

    private fun formatDistanceForUi(meters: Double): String {
        return "総移動距離: ${formatDistance(meters)}"
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format(Locale.US, "%.2f km", meters / 1000.0)
        } else {
            String.format(Locale.US, "%.1f m", meters)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _uiState.update { it.copy(elapsedTimeText = formatTime(0)) }
            while (isActive) {
                val elapsedTimeInSeconds = (System.currentTimeMillis() - startTime) / 1000
                _uiState.update { it.copy(elapsedTimeText = formatTime(elapsedTimeInSeconds)) }
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
