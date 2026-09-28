package com.example.smartstudent.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartstudent.ui.components.MascotBird
import com.example.smartstudent.ui.components.MascotMood

/**
 * Shown for the brief moment it takes to check whether Firebase already has a
 * signed-in user (auto-login), before the NavHost routes to Welcome, email
 * verification, or straight into the Dashboard.
 */
@Composable
fun SplashScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            MascotBird(size = 96.dp, mood = MascotMood.NEUTRAL)
        }
    }
}
