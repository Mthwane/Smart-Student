package com.example.smartstudent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartstudent.theme.StudentBeige100
import com.example.smartstudent.theme.StudentBrown600
import com.example.smartstudent.theme.StudentBrown800

/**
 * Top bar shared by every main tab: small-caps "SMARTSTUDENT" wordmark + a piggy
 * icon badge on the left, screen title below it, and settings + avatar actions
 * on the right — mirrors the Figma header used on Dashboard/Activity/Goals/Insights.
 */
@Composable
fun AppHeader(
    title: String,
    onSettingsClick: (() -> Unit)? = null,
    avatar: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .size(34.dp)
                    .background(StudentBeige100, CircleShape),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🐷", fontSize = 16.sp, textAlign = TextAlign.Center)
            }
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    "SMARTSTUDENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudentBrown600,
                    letterSpacing = 1.2.sp
                )
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = StudentBrown800
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onSettingsClick != null) {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Filled.Tune, contentDescription = "Settings", tint = StudentBrown800)
                }
            }
            avatar()
        }
    }
}
