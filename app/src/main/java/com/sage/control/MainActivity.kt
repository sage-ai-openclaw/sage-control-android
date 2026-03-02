package com.sage.control

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sage.control.ui.screens.ChatScreen
import com.sage.control.ui.screens.LoginScreen
import com.sage.control.ui.screens.SessionListScreen
import com.sage.control.ui.theme.SageControlTheme
import com.sage.control.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SageControlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SageControlApp()
                }
            }
        }
    }
}

@Composable
fun SageControlApp(authViewModel: AuthViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    // Auto-connect if already logged in
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            authViewModel.autoConnect()
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "sessions" else "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("sessions") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("sessions") {
            SessionListScreen(
                onSessionClick = { sessionKey ->
                    navController.navigate("chat/$sessionKey")
                },
                onMenuClick = {
                    // Open drawer or settings
                }
            )
        }

        composable(
            route = "chat/{sessionKey}",
            arguments = listOf(navArgument("sessionKey") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionKey = backStackEntry.arguments?.getString("sessionKey") ?: return@composable
            ChatScreen(
                sessionKey = sessionKey,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}