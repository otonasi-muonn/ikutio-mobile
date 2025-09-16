package com.example.ikutio_mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ikutio_mobile.ui.login.LoginScreen
import com.example.ikutio_mobile.ui.main.MainScreen

// アプリ内の画面を定義する
sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Main : Screen("main_screen")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route // 最初に表示する画面
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // ログイン成功時にメイン画面に遷移し、
                    // ログイン画面には戻れないようにする
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(route = Screen.Main.route) {
            MainScreen()
        }
    }
}