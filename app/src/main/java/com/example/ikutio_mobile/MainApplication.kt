package com.example.ikutio_mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // 通知チャンネルを作成
        val name = "位置情報サービス"
        val descriptionText = "バックグラウンドで位置情報を記録しています"
        val importance = NotificationManager.IMPORTANCE_LOW // あまり邪魔にならない優先度
        val channel = NotificationChannel(
            LOCATION_SERVICE_CHANNEL_ID,
            name,
            importance
        ).apply {
            description = descriptionText
        }

        // チャンネルをOSに登録
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val LOCATION_SERVICE_CHANNEL_ID = "location_service_channel"
    }
}