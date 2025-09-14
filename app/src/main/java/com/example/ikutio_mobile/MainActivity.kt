package com.example.ikutio_mobile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ikutio_mobile.ui.login.LoginViewModel
import com.example.ikutio_mobile.ui.theme.IkutiomobileTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // HiltをActivityで使うためのおまじない
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IkutiomobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen()
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel() // Hiltを使ってViewModelを取得
) {
    // ViewModelのuiStateを監視し、変更があれば自動で再描画する
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ログイン成功時の処理
    if (uiState.loginSuccess) {
        // TODO: メイン画面へ遷移する処理を後で追加
        Toast.makeText(context, "ログイン成功！", Toast.LENGTH_SHORT).show()
    }

    // 画面全体のレイアウト
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text("ログイン", style = MaterialTheme.typography.headlineMedium)

            // メールアドレス入力欄
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text("メールアドレス") },
                singleLine = true
            )

            // パスワード入力欄
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("パスワード") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ログインボタン
            Button(
                onClick = { viewModel.login() },
                enabled = !uiState.isLoading // ローディング中はボタンを無効化
            ) {
                Text("ログイン")
            }

            // エラーメッセージ表示
            uiState.loginError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // ローディングインジケータ
        if (uiState.isLoading) {
            CircularProgressIndicator()
        }
    }
}