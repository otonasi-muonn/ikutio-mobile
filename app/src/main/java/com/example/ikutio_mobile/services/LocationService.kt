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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationCallback: LocationCallback

    companion object {
        const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 15000 // 15秒
        const val NOTIFICATION_ID = 1
        private const val TAG = "LocationService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
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
            Log.e(TAG, "Location permission not granted. Stopping service.")
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
                    Log.d(TAG, "Raw location received: Lat ${location.latitude}, Lng ${location.longitude}, Time: ${location.time}")
                    locationRepository.updateLatestRawLocation(location.latitude, location.longitude)

                    serviceScope.launch {
                        try {
                            locationRepository.addLocationPoint(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                timestamp = location.time
                            )
                            Log.d(TAG, "Raw location point saved to DB.")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving raw location point to DB", e)
                        }
                    }
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        Log.d(TAG, "Location updates started.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called. Stopping location updates.")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        Log.d(TAG, "LocationService destroyed.")
    }
}
