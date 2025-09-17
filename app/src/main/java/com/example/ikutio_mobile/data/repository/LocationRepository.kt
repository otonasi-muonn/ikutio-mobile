package com.example.ikutio_mobile.data.repository

import com.example.ikutio_mobile.data.local.dao.LocationDao
import com.example.ikutio_mobile.data.local.entity.LocationPointEntity
import com.example.ikutio_mobile.data.remote.GameApiService
import com.example.ikutio_mobile.data.remote.dto.PathDataItem
import com.example.ikutio_mobile.data.remote.dto.PathDataRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao,
    private val gameApiService: GameApiService
) {

    // 最新の生座標 (緯度, 経度)
    private val _latestRawLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val latestRawLocation: StateFlow<Pair<Double, Double>?> = _latestRawLocation.asStateFlow()

    // 最新の正規化後座標 (緯度, 経度)
    private val _latestNormalizedLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val latestNormalizedLocation: StateFlow<Pair<Double, Double>?> = _latestNormalizedLocation.asStateFlow()

    // リポジトリ初期化時にDBから最新の正規化座標を読み込む
    init {
        // ViewModel などが初期値をすぐに受け取れるように、
        // ここではブロッキングしないように別コルーチンで実行
        CoroutineScope(Dispatchers.IO).launch { // IOディスパッチャでDBアクセス
            val latestPoint = locationDao.getLatestLocationPoint()
            latestPoint?.let {
                _latestNormalizedLocation.value = Pair(it.latitude, it.longitude)
            }
        }
    }

    fun updateLatestRawLocation(latitude: Double, longitude: Double) {
        _latestRawLocation.value = Pair(latitude, longitude)
    }

    fun updateLatestNormalizedLocation(latitude: Double?, longitude: Double?) {
        if (latitude != null && longitude != null) {
            _latestNormalizedLocation.value = Pair(latitude, longitude)
        } else {
            _latestNormalizedLocation.value = null // 正規化失敗・該当なしの場合
        }
    }

    suspend fun addLocationPoint(latitude: Double, longitude: Double, timestamp: Long) {
        val point = LocationPointEntity(
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp
        )
        locationDao.insertLocationPoint(point)
        // DBに新しい正規化座標が追加されたら、StateFlowも更新する
        updateLatestNormalizedLocation(latitude, longitude)
    }

    suspend fun getAllLocationPoints(): List<LocationPointEntity> {
        return locationDao.getAllLocationPoints()
    }

    suspend fun clearAllLocationPoints() {
        locationDao.deleteAllLocationPoints()
        // ローカルデータがクリアされたら、StateFlowも初期値に戻す（nullまたは適切な初期値）
        _latestNormalizedLocation.value = null // または他の初期値
        _latestRawLocation.value = null
    }

    suspend fun sendPathDataToServer() {
        val localPoints = locationDao.getAllLocationPoints()

        if (localPoints.isEmpty()) {
            // 送信するデータがない場合は何もしない
            return
        }

        val pathDataItems = localPoints.map { entity ->
            PathDataItem(
                latitude = entity.latitude,
                longitude = entity.longitude,
                timestamp = formatTimestamp(entity.timestamp)
            )
        }

        val request = PathDataRequest(pathData = pathDataItems)

        val response = gameApiService.sendPathData(request)

        if (response.isSuccessful) {
            locationDao.deleteAllLocationPoints()
            // 送信成功後もStateFlowをクリア
            _latestNormalizedLocation.value = null
            _latestRawLocation.value = null
        } else {
            // TODO: より具体的なエラーハンドリング (例: UIに通知する、リトライするなど)
            throw Exception("API call failed with code: ${response.code()}")
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestamp))
    }
}
