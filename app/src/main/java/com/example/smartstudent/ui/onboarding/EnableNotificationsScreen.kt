package com.example.smartstudent.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartstudent.theme.CategoryPeach
import com.example.smartstudent.theme.StudentGray600
import com.example.smartstudent.theme.StudentShapes
import com.example.smartstudent.ui.components.PrimaryPillButton
import com.example.smartstudent.ui.components.SecondaryPillButton
import androidx.compose.foundation.layout.systemBarsPadding

@Composable
fun EnableNotificationsScreen(
    onNotNow: () -> Unit,
    onEnable: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(48.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(CategoryPeach, StudentShapes.large),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Enable notifications", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Get reminders and updates about your budget and savings progress. You can turn this off any time in your phone\u2019s settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = StudentGray600
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecondaryPillButton(text = "Not now", onClick = onNotNow, modifier = Modifier.weight(1f))
            PrimaryPillButton(text = "Enable", onClick = onEnable, modifier = Modifier.weight(1f))
        }
    }
}
