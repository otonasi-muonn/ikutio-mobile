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
import androidx.compose.runtime.remember
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
    val TAG = "PermissionFlow"

    val permissionsToRequest = remember {
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val allPermissionsGranted = permissions.values.all { it }
            if (allPermissionsGranted) {
                Log.d(TAG, "All permissions granted after request. Starting service.")
                context.startForegroundService(Intent(context, LocationService::class.java))
                viewModel.startTimerAndUpdateState()
            } else {
                Log.w(TAG, "Not all permissions were granted after request.")
                Toast.makeText(context, "機能の利用に必要な権限が許可されませんでした。", Toast.LENGTH_LONG).show()
            }
        }
    )

    // --- UIの定義 ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "ステータス: ${uiState.statusText}", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "経過時間: ${uiState.elapsedTimeText}", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = uiState.rawLocationText, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = uiState.normalizedLocationText, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = uiState.totalDistanceText, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                Log.d(TAG, "--- Service Button Clicked ---")
                if (uiState.isServiceRunning) {
                    Log.d(TAG, "Stopping service.")
                    context.stopService(Intent(context, LocationService::class.java))
                    viewModel.stopTimerAndUpdateState()
                } else {
                    Log.d(TAG, "Checking permissions to start service...")
                    val allPermissionsGranted = permissionsToRequest.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (allPermissionsGranted) {
                        Log.d(TAG, "All permissions already granted. Starting service.")
                        context.startForegroundService(Intent(context, LocationService::class.java))
                        viewModel.startTimerAndUpdateState()
                    } else {
                        Log.d(TAG, "Requesting permissions...")
                        launcher.launch(permissionsToRequest)
                    }
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
