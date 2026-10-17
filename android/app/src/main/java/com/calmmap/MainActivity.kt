package com.calmmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.calmmap.ui.onboarding.SplashScreen
import com.calmmap.ui.theme.CalmMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalmMapTheme {
                SplashScreen()
            }
        }
    }
}
