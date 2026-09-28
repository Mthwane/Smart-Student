package com.example.smartstudent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.smartstudent.theme.SmartStudentTheme
import android.graphics.Color
import androidx.activity.SystemBarStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            SmartStudentApp()
        }
    }
}

@Composable
fun SmartStudentApp() {
    SmartStudentTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            SmartStudentNavHost()
        }
    }
}
