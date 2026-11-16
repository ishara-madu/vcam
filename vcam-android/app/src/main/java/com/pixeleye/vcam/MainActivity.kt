package com.pixeleye.vcam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pixeleye.vcam.ui.MainScreen
import com.pixeleye.vcam.ui.theme.VcamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VcamTheme {
                MainScreen()
            }
        }
    }
}
