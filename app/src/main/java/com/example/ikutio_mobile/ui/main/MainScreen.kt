package com.example.ikutio_mobile.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ikutio_mobile.services.LocationService

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val TAG = "PermissionFlow" // ログ用のタグ

    lateinit var checkPermissionsAndLaunchServiceIfNeeded: () -> Unit

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { permissions ->
                Log.d(TAG, "--- onResult called ---")
                Log.d(TAG, "Permissions result: $permissions")
                val allPermissionsGrantedInThisRequest = permissions.values.all { it }
                Log.d(TAG, "All permissions granted in this specific request: $allPermissionsGrantedInThisRequest")

                if (allPermissionsGrantedInThisRequest) {
                    Log.d(TAG, "All granted in this request, calling checkPermissionsAndLaunchServiceIfNeeded again.")
                    checkPermissionsAndLaunchServiceIfNeeded()
                } else {
                    Log.d(TAG, "Not all permissions granted in this request. Showing Toast.")
                    Toast.makeText(context, "必要な権限が許可されませんでした。", Toast.LENGTH_LONG).show()
                }
            }
        )

    checkPermissionsAndLaunchServiceIfNeeded = {
        Log.d(TAG, "--- checkPermissionsAndLaunchServiceIfNeeded called ---")

        val foregroundPermissionsArray = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val backgroundPermissionString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else { null }
        val notificationPermissionString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else { null }

        val isNotificationGranted = notificationPermissionString?.let { isPermissionGranted(context, it) } ?: true
        val areForegroundGranted = arePermissionsGranted(context, foregroundPermissionsArray)
        val isBackgroundGranted = backgroundPermissionString?.let { isPermissionGranted(context, it) } ?: true

        Log.d(TAG, "Current Permission States:")
        Log.d(TAG, "  Notification: Granted = $isNotificationGranted (Needed: ${notificationPermissionString != null})")
        Log.d(TAG, "  Foreground: Granted = $areForegroundGranted")
        Log.d(TAG, "  Background: Granted = $isBackgroundGranted (Needed: ${backgroundPermissionString != null})")

        when {
            notificationPermissionString != null && !isNotificationGranted -> {
                Log.d(TAG, "Requesting Notification permission: $notificationPermissionString")
                requestPermissionLauncher.launch(arrayOf(notificationPermissionString))
            }
            !areForegroundGranted -> {
                Log.d(TAG, "Requesting Foreground permissions: ${foregroundPermissionsArray.joinToString()}")
                requestPermissionLauncher.launch(foregroundPermissionsArray)
            }
            backgroundPermissionString != null && !isBackgroundGranted -> {
                Log.d(TAG, "Requesting Background permission: $backgroundPermissionString")
                if (areForegroundGranted) {
                    requestPermissionLauncher.launch(arrayOf(backgroundPermissionString))
                } else {
                    Log.w(TAG, "Background permission needed, but foreground not granted yet. Should not happen if logic is correct.")
                }
            }
            else -> {
                Log.d(TAG, "All necessary permissions are granted. Starting service.")
                context.startForegroundService(Intent(context, LocationService::class.java))
                viewModel.startTimerAndUpdateState()
            }
        }
    }

    // --- UIの定義 ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), // パディングを追加して見やすくする
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "ステータス: ${uiState.statusText}", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "経過時間: ${uiState.elapsedTimeText}", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(16.dp)) // 間隔を調整

        // ★★★ 生の座標と正規化された座標を表示するTextを追加 ★★★
        Text(text = uiState.rawLocationText, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = uiState.normalizedLocationText, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(32.dp))
        // ★★★ ここまで追加 ★★★

        Button(
            onClick = {
                Log.d(TAG, "--- Service Button Clicked ---")
                if (uiState.isServiceRunning) {
                    Log.d(TAG, "Stopping service.")
                    context.stopService(Intent(context, LocationService::class.java))
                    viewModel.stopTimerAndUpdateState()
                } else {
                    Log.d(TAG, "Attempting to start service, calling checkPermissionsAndLaunchServiceIfNeeded.")
                    checkPermissionsAndLaunchServiceIfNeeded()
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp)
        ) {
            Text(text = if (uiState.isServiceRunning) "サービス終了" else "サービス起動")
        }
    }
}

// 単一の権限チェック用ヘルパー
private fun isPermissionGranted(context: Context, permission: String): Boolean {
    val result = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    Log.d("PermissionFlow", "isPermissionGranted for $permission: $result")
    return result
}

// 複数の権限チェック用ヘルパー
private fun arePermissionsGranted(context: Context, permissions: Array<String>): Boolean {
    if (permissions.isEmpty()) {
        Log.d("PermissionFlow", "arePermissionsGranted for empty array: true")
        return true
    }
    val result = permissions.all { isPermissionGranted(context, it) }
    Log.d("PermissionFlow", "arePermissionsGranted for ${permissions.joinToString()}: $result")
    return result
}
