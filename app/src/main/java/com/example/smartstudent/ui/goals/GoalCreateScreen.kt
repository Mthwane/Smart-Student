package com.example.smartstudent.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smartstudent.domain.model.GoalKind
import com.example.smartstudent.theme.StudentGray600
import com.example.smartstudent.ui.components.PrimaryPillButton
import com.example.smartstudent.ui.components.SecondaryPillButton
import com.example.smartstudent.ui.components.StudentTextField
import com.example.smartstudent.ui.components.TextLinkButton
import com.example.smartstudent.util.formScreen
import com.example.smartstudent.util.parseAmount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val GoalEmojis = listOf("\uD83C\uDFAF", "\uD83D\uDCBB", "\u2708\uFE0F", "\uD83D\uDEE1\uFE0F", "\uD83C\uDFE0", "\uD83D\uDCDA", "\uD83E\uDDFE", "\uD83C\uDF81")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalCreateScreen(
    kind: GoalKind,
    onSave: (name: String, targetAmount: Double, description: String, emoji: String, dueDate: LocalDate?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var targetAmount by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf(if (kind == GoalKind.SMART_BILL) "\uD83E\uDDFE" else "\uD83C\uDFAF") }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val parsedTarget = parseAmount(targetAmount)
    val dueDateInPast = dueDate?.isBefore(LocalDate.now()) == true

    val title = if (kind == GoalKind.SMART_BILL) "New Smart bill" else "New savings goal"
    val nameLabel = if (kind == GoalKind.SMART_BILL) "What's the bill for?" else "What are you saving for?"

    Column(modifier = Modifier.formScreen().padding(24.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "A short description helps you remember why this goal matters — you'll be able to tap it later to see it again.",
            style = MaterialTheme.typography.bodyMedium,
            color = StudentGray600
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GoalEmojis.forEach { candidate ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (candidate == emoji) MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { emoji = candidate },
                    contentAlignment = Alignment.Center
                ) { Text(candidate) }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        StudentTextField(value = name, onValueChange = { name = it }, label = nameLabel)
        Spacer(modifier = Modifier.height(14.dp))
        StudentTextField(
            value = targetAmount,
            onValueChange = { targetAmount = it },
            label = "Target amount (R)",
            keyboardType = KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(14.dp))
        StudentTextField(
            value = description,
            onValueChange = { description = it },
            label = "Description (optional)"
        )
        Spacer(modifier = Modifier.height(14.dp))

        SecondaryPillButton(
            text = dueDate?.let { "Target date: $it" } ?: "Add target date (optional)",
            onClick = { showDatePicker = true }
        )
        if (dueDate != null) {
            TextLinkButton(text = "Clear date", onClick = { dueDate = null })
        }
        if (dueDateInPast) {
            Text(
                "Pick a date in the future.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        PrimaryPillButton(
            text = "Create goal",
            onClick = {
                parsedTarget?.let {
                    submitted = true
                    onSave(name.trim(), it, description.trim(), emoji, dueDate)
                }
            },
            enabled = name.isNotBlank() && parsedTarget != null && !dueDateInPast && !submitted,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showDatePicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        dueDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}
