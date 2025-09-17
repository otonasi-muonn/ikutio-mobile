package com.example.ikutio_mobile.services

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.ikutio_mobile.MainApplication.Companion.LOCATION_SERVICE_CHANNEL_ID
import com.example.ikutio_mobile.R
import com.example.ikutio_mobile.data.remote.MapsApiService
import com.example.ikutio_mobile.data.repository.LocationRepository
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var fusedLocationClient: FusedLocationProviderClient
    @Inject
    lateinit var locationRepository: LocationRepository
    @Inject
    lateinit var mapsApiService: MapsApiService // MapsApiServiceを注入

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationCallback: LocationCallback

    companion object {
        const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 15000 // 15秒
        const val NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        startLocationUpdates()
        return START_STICKY
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, LOCATION_SERVICE_CHANNEL_ID)
            .setContentTitle("経路を記録中")
            .setContentText("バックグラウンドで位置情報を取得しています...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "Location permission not granted.")
            stopSelf()
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_IN_MILLISECONDS
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    Log.d("LocationService", "Raw location: ${location.latitude}, ${location.longitude}")
                    // ★ 生の座標をRepositoryに通知
                    locationRepository.updateLatestRawLocation(location.latitude, location.longitude)

                    serviceScope.launch {
                        try {
                            val metadataResponse = mapsApiService.getStreetViewMetadata(
                                location = "${location.latitude},${location.longitude}"
                            )
                            if (metadataResponse.status == "OK" && metadataResponse.location != null) {
                                val normalizedLat = metadataResponse.location.lat
                                val normalizedLng = metadataResponse.location.lng
                                Log.d("LocationService", "Normalized location: $normalizedLat, $normalizedLng")

                                locationRepository.updateLatestNormalizedLocation(normalizedLat, normalizedLng)
                                // DBには正規化後の座標を保存
                                locationRepository.addLocationPoint(
                                    latitude = normalizedLat,
                                    longitude = normalizedLng,
                                    timestamp = System.currentTimeMillis()
                                )
                            } else {
                                Log.w("LocationService", "No Street View found or error: ${metadataResponse.status}. Skipping point.")
                                // ★ 正規化失敗/該当なしをRepositoryに通知
                                locationRepository.updateLatestNormalizedLocation(null, null)
                            }
                        } catch (e: Exception) {
                            Log.e("LocationService", "Error calling Maps API or saving location", e)
                            locationRepository.updateLatestNormalizedLocation(null, null)
                        }
                    }
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }
}