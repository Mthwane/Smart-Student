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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smartstudent.theme.CategoryCoral
import com.example.smartstudent.theme.CategoryMint
import com.example.smartstudent.theme.CategoryPeach
import com.example.smartstudent.theme.CategoryPurple
import com.example.smartstudent.theme.CategorySky
import com.example.smartstudent.theme.StudentGray600
import com.example.smartstudent.ui.components.MascotBird
import com.example.smartstudent.ui.components.MascotMood
import com.example.smartstudent.ui.components.PrimaryPillButton
import com.example.smartstudent.ui.components.SecondaryPillButton

@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onSignUp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        WelcomeHeroArt(modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Reach for your\ngoals effortlessly",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Whatever you're after, we help make it happen. Save without thinking about it, and track every rand across your student life.",
            style = MaterialTheme.typography.bodyLarge,
            color = StudentGray600
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecondaryPillButton(
                text = "Log in",
                onClick = onLogin,
                modifier = Modifier.weight(1f)
            )
            PrimaryPillButton(
                text = "Sign up",
                onClick = onSignUp,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Simple abstracted version of the reference art: a ring of soft-colored
 * category badges around a central accent shape, built from vector icons
 * rather than a bespoke illustration.
 */
@Composable
private fun WelcomeHeroArt(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center anchor: the mascot, replacing the old plain circle
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(CategoryCoral.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MascotBird(size = 108.dp, mood = MascotMood.HAPPY)
        }

        HeroBadge(
            icon = Icons.Filled.DirectionsCar,
            color = CategoryPurple,
            modifier = Modifier.align(Alignment.TopStart)
        )
        HeroBadge(
            icon = Icons.Filled.ShoppingBag,
            color = CategoryMint,
            modifier = Modifier.align(Alignment.TopEnd)
        )
        HeroBadge(
            icon = Icons.Filled.LocalCafe,
            color = CategorySky,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
        HeroBadge(
            icon = Icons.Filled.Work,
            color = CategoryPeach,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun HeroBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(84.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF1A1A1A))
    }
}
