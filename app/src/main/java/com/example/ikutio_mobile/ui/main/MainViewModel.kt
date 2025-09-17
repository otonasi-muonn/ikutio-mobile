package com.example.ikutio_mobile.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ikutio_mobile.data.repository.LocationRepository
import com.example.ikutio_mobile.data.repository.InsufficientPointsException
import com.example.ikutio_mobile.data.repository.NoProcessablePathDataException
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
private const val INITIAL_RAW_LOCATION_TEXT = "生の座標: ---"

data class MainUiState(
    val isServiceRunning: Boolean = false,
    val statusText: String = "待機中",
    val elapsedTimeText: String = INITIAL_TIME,
    val rawLocationText: String = INITIAL_RAW_LOCATION_TEXT,
    val totalDistanceText: String = INITIAL_DISTANCE_TEXT
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private val TAG = "MainViewModel"

    init {
        locationRepository.latestRawLocation
            .onEach { locationPair ->
                val text = locationPair?.let { (lat, lng) -> "生の座標: ${String.format(Locale.US, "%.8f, %.8f", lat, lng)}" } ?: INITIAL_RAW_LOCATION_TEXT
                _uiState.update { it.copy(rawLocationText = text) }
            }.launchIn(viewModelScope)

        locationRepository.processedPathDistance
            .onEach { distance ->
                val distanceText = distance?.let { formatDistanceForUi(it) } ?: INITIAL_DISTANCE_TEXT
                _uiState.update { it.copy(totalDistanceText = distanceText) }
                Log.d(TAG, "Processed path distance updated in UI: $distanceText")
            }.launchIn(viewModelScope)
    }

    fun startTimerAndUpdateState() {
        locationRepository.resetProcessedPathDistance()
        _uiState.update { it.copy(isServiceRunning = true, statusText = "記録中") }
        startTimer()
    }

    fun stopTimerAndUpdateState() {
        _uiState.update { it.copy(statusText = "経路データを処理中...") }
        stopTimer()
        viewModelScope.launch {
            try {
                locationRepository.sendPathDataToServer()
                _uiState.update {
                    it.copy(
                        isServiceRunning = false,
                        statusText = "送信完了"
                    )
                }
            } catch (e: InsufficientPointsException) {
                Log.w(TAG, "Processing failed: ${e.message}")
                _uiState.update {
                    it.copy(
                        isServiceRunning = false,
                        statusText = e.message ?: "座標が不足しています"
                    )
                }
            } catch (e: NoProcessablePathDataException) {
                Log.w(TAG, "Processing failed: ${e.message}")
                _uiState.update {
                    it.copy(
                        isServiceRunning = false,
                        statusText = e.message ?: "有効な経路データなし"
                    )
                }
            }
            catch (e: Exception) {
                Log.e(TAG, "Processing or sending failed with general exception", e)
                _uiState.update {
                    it.copy(
                        isServiceRunning = false,
                        statusText = "処理または送信失敗: ${e.message?.take(30) ?: "不明なエラー"}"
                    )
                }
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
        _uiState.update { it.copy(elapsedTimeText = INITIAL_TIME) }
    }

    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }
}
