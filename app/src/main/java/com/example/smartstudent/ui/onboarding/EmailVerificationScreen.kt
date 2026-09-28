package com.example.smartstudent.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartstudent.theme.CategoryMint
import com.example.smartstudent.theme.StudentGray600
import com.example.smartstudent.theme.StudentShapes
import com.example.smartstudent.ui.components.PrimaryPillButton
import com.example.smartstudent.ui.components.TextLinkButton

@Composable
fun EmailVerificationScreen(
    email: String,
    onResend: () -> Unit,
    onIveVerified: () -> Unit,
    checking: Boolean = false,
    infoMessage: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(48.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(CategoryMint, StudentShapes.large),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MarkEmailUnread, contentDescription = null, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Verify your email", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "We sent a verification link to:",
                style = MaterialTheme.typography.bodyLarge,
                color = StudentGray600
            )
            Text(email, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Open the email and tap the link, then come back here and tap \"I've verified my email.\"",
                style = MaterialTheme.typography.bodyMedium,
                color = StudentGray600
            )

            if (infoMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(infoMessage, style = MaterialTheme.typography.bodyMedium, color = StudentGray600)
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextLinkButton(text = "Resend email", onClick = onResend)
        }

        PrimaryPillButton(
            text = "I've verified my email",
            onClick = onIveVerified,
            loading = checking,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )
    }
}
