package com.cuchieman.tamatask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cuchieman.tamatask.navigation.Screen
import com.cuchieman.tamatask.ui.screens.MainScreen
import com.cuchieman.tamatask.ui.screens.SplashScreen
import com.cuchieman.tamatask.ui.theme.TamaTaskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TamaTaskTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route
                ) {
                    composable(
                        route = Screen.Splash.route,
                        exitTransition = {
                            fadeOut(animationSpec = tween(300))
                        }
                    ) {
                        SplashScreen(
                            onStartClick = {
                                navController.navigate(Screen.Main.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(
                        route = Screen.Main.route,
                        enterTransition = {
                            fadeIn(animationSpec = tween(300))
                        }
                    ) {
                        MainScreen()
                    }
                }
            }
        }
    }
}
