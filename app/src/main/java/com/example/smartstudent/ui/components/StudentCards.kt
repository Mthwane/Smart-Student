package com.example.smartstudent.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smartstudent.theme.CardShape
import com.example.smartstudent.theme.StudentGray200
import com.example.smartstudent.theme.StudentGray600
import com.example.smartstudent.theme.StudentGreen
import com.example.smartstudent.theme.StudentGreenLight

/** Rounded outlined row-card, used for goal-type pickers and list rows. */
@Composable
fun OutlinedRowCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit = {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = StudentGray600
        )
    },
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudentGray200, CardShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        leading()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = StudentGray600)
        }
        trailing()
    }
}

/** Circular icon badge — used as the leading element in OutlinedRowCard and goal cards. */
@Composable
fun IconBadge(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Goal progress card — emoji badge, name, amount saved vs target, progress bar,
 *  and a tap-to-expand description. Tapping anywhere on the card (or the chevron)
 *  toggles the description; there's no separate "detail" destination.
 */
@Composable
fun GoalProgressCard(
    emoji: String,
    name: String,
    savedAmount: String,
    targetAmount: String,
    progress: Float,
    modifier: Modifier = Modifier,
    description: String = "",
    expanded: Boolean = false,
    onToggleExpanded: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudentGray200, CardShape)
            .then(
                if (description.isNotBlank() && onToggleExpanded != null)
                    Modifier.clickable { onToggleExpanded() }
                else Modifier
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(backgroundColor = StudentGreenLight) {
                Text(emoji, style = MaterialTheme.typography.titleLarge)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$savedAmount of $targetAmount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudentGray600
                )
            }
            if (description.isNotBlank()) {
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Hide description" else "Show description",
                    tint = StudentGray600
                )
            }
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = StudentGreen,
            trackColor = StudentGray200,
        )

        AnimatedVisibility(visible = expanded && description.isNotBlank()) {
            Column {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = StudentGray600)
            }
        }
    }
}
