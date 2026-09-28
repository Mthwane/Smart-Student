package com.example.smartstudent.ui.goals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smartstudent.domain.model.GoalKind
import com.example.smartstudent.theme.StudentGray600
import com.example.smartstudent.ui.components.PrimaryPillButton
import com.example.smartstudent.ui.components.StudentTextField

@Composable
fun GoalCreateScreen(
    kind: GoalKind,
    onSave: (name: String, targetAmount: Double, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val title = if (kind == GoalKind.SMART_BILL) "New Smart bill" else "New savings goal"
    val nameLabel = if (kind == GoalKind.SMART_BILL) "What's the bill for?" else "What are you saving for?"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "A short description helps you remember why this goal matters — you'll be able to tap it later to see it again.",
            style = MaterialTheme.typography.bodyMedium,
            color = StudentGray600
        )
        Spacer(modifier = Modifier.height(20.dp))

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

        Spacer(modifier = Modifier.height(24.dp))
        PrimaryPillButton(
            text = "Create goal",
            onClick = {
                val parsedAmount = targetAmount.toDoubleOrNull() ?: 0.0
                onSave(name, parsedAmount, description)
            },
            enabled = name.isNotBlank() && (targetAmount.toDoubleOrNull() ?: 0.0) > 0.0,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
