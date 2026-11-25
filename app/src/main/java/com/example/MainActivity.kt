package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.ai.configureAppCheck
import com.example.ui.NovaCartApp
import com.example.ui.theme.NovaCartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge and render NovaCart Intelligence UI
        enableEdgeToEdge()
        setContent {
            NovaCartTheme {
                NovaCartApp()
            }
        }
    }
}
