package com.example.smartstudent.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smartstudent.domain.model.GoalKind
import com.example.smartstudent.domain.model.SavingsGoal
import com.example.smartstudent.theme.CardShape
import com.example.smartstudent.theme.CategoryChartPalette
import com.example.smartstudent.theme.PillShape
import com.example.smartstudent.theme.StudentBeige100
import com.example.smartstudent.theme.StudentBlack
import com.example.smartstudent.theme.StudentBrown600
import com.example.smartstudent.theme.StudentBrown800
import com.example.smartstudent.theme.StudentCardBlack
import com.example.smartstudent.theme.StudentGold
import com.example.smartstudent.theme.StudentGreen
import com.example.smartstudent.theme.StudentRed
import com.example.smartstudent.theme.StudentWhite
import com.example.smartstudent.ui.components.AppHeader
import com.example.smartstudent.ui.components.MascotBird
import com.example.smartstudent.ui.components.MascotMood
import com.example.smartstudent.ui.components.StudentTextField

@Composable
fun GoalsScreen(
    goals: List<SavingsGoal>,
    onAddGoal: () -> Unit,
    onAddMoney: (goalId: String, amount: Double) -> Unit,
    onDeleteGoal: (goalId: String) -> Unit
) {
    var goalPendingAddMoney by remember { mutableStateOf<SavingsGoal?>(null) }
    var goalPendingDelete by remember { mutableStateOf<SavingsGoal?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppHeader(title = "Savings Goals") {
                    MascotBird(size = 34.dp, mood = MascotMood.HAPPY)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Track target funds for the semester",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudentBrown600,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onAddGoal,
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = StudentGold, contentColor = StudentBlack)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Goal", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (goals.isNotEmpty()) {
            item { TotalVaultCard(goals) }
        }

        item {
            Text("Active Targets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (goals.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No goals yet", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap \"New Goal\" to start your first savings target",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudentBrown600
                    )
                }
            }
        } else {
            items(goals, key = { it.id }) { goal ->
                GoalTargetCard(
                    goal = goal,
                    onAddMoney = { goalPendingAddMoney = goal },
                    onDelete = { goalPendingDelete = goal }
                )
            }
        }

        item { StartNewVaultRow(onAddGoal = onAddGoal) }
    }

    goalPendingAddMoney?.let { goal ->
        AddMoneyDialog(
            goalName = goal.name,
            onDismiss = { goalPendingAddMoney = null },
            onConfirm = { amount ->
                onAddMoney(goal.id, amount)
                goalPendingAddMoney = null
            }
        )
    }

    goalPendingDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalPendingDelete = null },
            title = { Text("Delete \"${goal.name}\"?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteGoal(goal.id)
                    goalPendingDelete = null
                }) { Text("Delete", color = StudentRed) }
            },
            dismissButton = {
                TextButton(onClick = { goalPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TotalVaultCard(goals: List<SavingsGoal>) {
    val totalSaved = goals.sumOf { it.savedAmount }
    val totalTarget = goals.sumOf { it.targetAmount }
    val fundedPct = if (totalTarget > 0) (totalSaved / totalTarget * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudentCardBlack, CardShape)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(StudentGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = StudentBlack, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("TOTAL VAULT BALANCE", style = MaterialTheme.typography.labelMedium, color = StudentTaupeOnBlack)
            }
            Row(
                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), PillShape).padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("$fundedPct% funded", style = MaterialTheme.typography.labelSmall, color = StudentGold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("R%,.2f".format(totalSaved), style = MaterialTheme.typography.displayLarge, color = StudentWhite)
            Text(
                " / R%,.2f".format(totalTarget),
                style = MaterialTheme.typography.titleMedium,
                color = StudentTaupeOnBlack,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Text(
            "${goals.size} active goal${if (goals.size == 1) "" else "s"} running this semester",
            style = MaterialTheme.typography.bodyMedium,
            color = StudentTaupeOnBlack
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Segmented stacked progress bar — one segment per goal, proportional to its target.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            goals.forEachIndexed { index, goal ->
                val weight = (goal.targetAmount / totalTarget.coerceAtLeast(1.0)).toFloat().coerceAtLeast(0.001f)
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxSize()
                        .background(CategoryChartPalette[index % CategoryChartPalette.size])
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            goals.take(4).forEachIndexed { index, goal ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CategoryChartPalette[index % CategoryChartPalette.size])
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${goal.name} (R%,.0f)".format(goal.savedAmount),
                        style = MaterialTheme.typography.labelSmall,
                        color = StudentTaupeOnBlack
                    )
                }
            }
        }
    }
}

private val StudentTaupeOnBlack = Color(0xFFBFBBB0)

private fun tagFor(goal: SavingsGoal): Pair<String, Color> {
    if (goal.kind == GoalKind.SMART_BILL) return "BUFFER" to Color(0xFF8C6A7A)
    val n = goal.name.lowercase()
    return when {
        "emergency" in n || "buffer" in n -> "PRIORITY" to StudentRed
        "laptop" in n || "phone" in n || "tech" in n || "computer" in n -> "TECH" to Color(0xFFC97B54)
        "trip" in n || "travel" in n || "break" in n || "holiday" in n -> "LIFESTYLE" to Color(0xFF8C6A7A)
        else -> "GOAL" to StudentGold
    }
}

private fun iconFor(goal: SavingsGoal) = when {
    "laptop" in goal.name.lowercase() || "tech" in goal.name.lowercase() -> Icons.Filled.Laptop
    "trip" in goal.name.lowercase() || "car" in goal.name.lowercase() || "break" in goal.name.lowercase() -> Icons.Filled.DirectionsCar
    "emergency" in goal.name.lowercase() -> Icons.Filled.Shield
    else -> Icons.Filled.Savings

}

@Composable
private fun GoalTargetCard(goal: SavingsGoal, onAddMoney: () -> Unit, onDelete: () -> Unit) {
    val (tagLabel, tagColor) = tagFor(goal)
    val achievedPct = (goal.progress * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudentWhite, CardShape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tagColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconFor(goal), contentDescription = null, tint = tagColor, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(goal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.clip(PillShape).background(tagColor.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(tagLabel, style = MaterialTheme.typography.labelSmall, color = tagColor, fontWeight = FontWeight.Bold)
                    }
                }
                goal.dueDate?.let {
                    Text("Target: $it", style = MaterialTheme.typography.bodyMedium, color = StudentBrown600)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("R%,.2f".format(goal.savedAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("of R%,.2f".format(goal.targetAmount), style = MaterialTheme.typography.labelSmall, color = StudentBrown600)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$achievedPct% Achieved", style = MaterialTheme.typography.labelMedium, color = StudentGreen, fontWeight = FontWeight.Bold)
            Text("R%,.2f to go".format((goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)), style = MaterialTheme.typography.labelMedium, color = StudentBrown600)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { goal.progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
            color = tagColor,
            trackColor = StudentBeige100
        )

        if (goal.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier.fillMaxWidth().background(StudentBeige100, RoundedCornerShape(12.dp)).padding(12.dp)
            ) {
                Text(goal.description, style = MaterialTheme.typography.bodyMedium, color = StudentBrown600)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onAddMoney,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(containerColor = StudentBlack, contentColor = StudentWhite),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Money", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.clip(CircleShape).background(StudentBeige100)
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete goal", tint = StudentBrown600)
            }
        }
    }
}

@Composable
private fun StartNewVaultRow(onAddGoal: () -> Unit) {
    Column {
        Text("What do you want to save for?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Choose a flexible vault tailored to student cash flow.",
            style = MaterialTheme.typography.bodyMedium,
            color = StudentBrown600
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TemplateCard(
                emoji = "🐷",
                title = "Savings Goal",
                subtitle = "Flexible target and automated micro-saves.",
                modifier = Modifier.weight(1f),
                onClick = onAddGoal
            )
            TemplateCard(
                emoji = "🛡️",
                title = "Smart Bill Buffer",
                subtitle = "Safeguard rent, meal plans, or tuition.",
                modifier = Modifier.weight(1f),
                onClick = onAddGoal
            )
        }
    }
}

@Composable
private fun TemplateCard(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .background(StudentWhite, CardShape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(StudentBeige100),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = StudentBrown600)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Start Goal →", style = MaterialTheme.typography.labelMedium, color = StudentGold, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AddMoneyDialog(
    goalName: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add money to \"$goalName\"") },
        text = {
            StudentTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = "Amount (R)",
                keyboardType = KeyboardType.Decimal
            )
        },
        confirmButton = {
            TextButton(
                onClick = { amount?.let { onConfirm(it) } },
                enabled = amount != null && amount > 0.0
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
