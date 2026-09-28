package com.example.smartstudent.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smartstudent.theme.PillShape
import com.example.smartstudent.theme.StudentGray200
import com.example.smartstudent.theme.StudentGray400
import androidx.compose.foundation.layout.size

/**
 * Full-width solid pill button — primary CTA style seen throughout the reference
 * onboarding flow ("Next", "Sign up", "Enable", "Get started").
 */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = PillShape,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = StudentGray200,
            disabledContentColor = StudentGray400
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Full-width outlined pill button — secondary CTA style ("Log in", "Not now").
 */
@Composable
fun SecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Text-only link-style button, used for "Cancel", "Resend", "Learn more" etc. */
@Composable
fun TextLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = com.example.smartstudent.theme.StudentGreen
) {
    androidx.compose.material3.TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text,
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
