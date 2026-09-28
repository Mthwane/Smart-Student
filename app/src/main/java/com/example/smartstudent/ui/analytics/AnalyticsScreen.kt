package com.example.smartstudent.ui.analytics

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.smartstudent.theme.CardShape
import com.example.smartstudent.theme.PillShape
import com.example.smartstudent.theme.StudentBeige100
import com.example.smartstudent.theme.StudentBlack
import com.example.smartstudent.theme.StudentBrown600
import com.example.smartstudent.theme.StudentBrown800
import com.example.smartstudent.theme.StudentGold
import com.example.smartstudent.theme.StudentGoldLight
import com.example.smartstudent.theme.StudentGreen
import com.example.smartstudent.theme.StudentRed
import com.example.smartstudent.theme.StudentWhite
import com.example.smartstudent.ui.components.AppHeader
import com.example.smartstudent.ui.components.CategoryPieChart
import com.example.smartstudent.ui.components.MascotBird
import com.example.smartstudent.ui.components.MascotMood
import com.example.smartstudent.ui.components.StudentTextField
import com.example.smartstudent.ui.components.buildPieSlices
import com.example.smartstudent.ui.main.HabitTipState

@Composable
fun AnalyticsScreen(
    monthlyIncome: Double,
    monthlyExpenses: Double,
    incomeToExpenseRatio: Double?,
    averageExpenseAmount: Double,
    transactionCount: Int,
    categorySpendTotals: Map<String, Double>,
    monthlyAllowance: Double,
    allowanceRemaining: Double,
    habitTipState: HabitTipState,
    onFetchHabitTip: () -> Unit,
    onSetAllowance: (Double) -> Unit
) {
    val overBudget = monthlyAllowance > 0.0 && allowanceRemaining < 0
    val usedFraction = if (monthlyAllowance > 0.0) (monthlyExpenses / monthlyAllowance).toFloat().coerceIn(0f, 1f) else 0f
    val usedPct = (usedFraction * 100).toInt()
    val slices = buildPieSlices(categorySpendTotals)
    var showAllowanceDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            AppHeader(title = "Analytics Insights") {
                MascotBird(size = 34.dp, mood = if (overBudget) MascotMood.CONCERNED else MascotMood.HAPPY)
            }
        }

        item {
            BudgetAllowanceCard(
                monthlyAllowance = monthlyAllowance,
                usedFraction = usedFraction,
                usedPct = usedPct,
                remaining = allowanceRemaining,
                overBudget = overBudget,
                onClick = { showAllowanceDialog = true }
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "Monthly Income",
                    value = "R%,.2f".format(monthlyIncome),
                    icon = Icons.Filled.ArrowDownward,
                    iconColor = StudentGreen,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Monthly Expenses",
                    value = "R%,.2f".format(monthlyExpenses),
                    icon = Icons.Filled.ArrowUpward,
                    iconColor = StudentRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "In/Out Ratio",
                    value = incomeToExpenseRatio?.let { "%.2fx".format(if (it > 0) 1 / it else 0.0) } ?: "—",
                    icon = Icons.Filled.Balance,
                    iconColor = StudentGold,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Avg. Ticket",
                    value = "R%,.2f".format(averageExpenseAmount),
                    icon = Icons.Filled.Receipt,
                    iconColor = StudentBrown800,
                    modifier = Modifier.weight(1f),
                    caption = "$transactionCount transactions"
                )
            }
        }

        item {
            Column {
                Text("BREAKDOWN", style = MaterialTheme.typography.labelSmall, color = StudentBrown600, fontWeight = FontWeight.Bold)
                Text("Category Spending", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                if (slices.isEmpty()) {
                    Text(
                        "No expenses logged this month yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudentBrown600
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().background(StudentWhite, CardShape).padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryPieChart(slices = slices)
                    }
                }
            }
        }

        item {
            GeminiSmartHabitCard(habitTipState = habitTipState, onFetchTip = onFetchHabitTip, hasData = categorySpendTotals.isNotEmpty())
        }
    }

    if (showAllowanceDialog) {
        SetAllowanceDialog(
            currentAmount = monthlyAllowance,
            onDismiss = { showAllowanceDialog = false },
            onConfirm = { amount ->
                onSetAllowance(amount)
                showAllowanceDialog = false
            }
        )
    }
}

@Composable
private fun BudgetAllowanceCard(
    monthlyAllowance: Double,
    usedFraction: Float,
    usedPct: Int,
    remaining: Double,
    overBudget: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudentWhite, CardShape)
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Monthly Budget Allowance", style = MaterialTheme.typography.titleMedium, color = StudentBrown600)
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (overBudget) StudentRed.copy(alpha = 0.14f) else StudentGreen.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    if (overBudget) "Over Budget" else "On Track",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (overBudget) StudentRed else StudentGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (monthlyAllowance > 0.0) {
            Text("R%,.2f".format(monthlyAllowance), style = MaterialTheme.typography.displayLarge, color = StudentBrown800)
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { usedFraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = if (overBudget) StudentRed else StudentBlack,
                trackColor = StudentBeige100
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (overBudget) "R%,.2f over budget".format(-remaining) else "R%,.2f remaining".format(remaining),
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudentBrown600
                )
                Text("$usedPct% used", style = MaterialTheme.typography.bodyMedium, color = StudentBrown600)
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Tap to set a monthly allowance and track your budget.",
                style = MaterialTheme.typography.bodyMedium,
                color = StudentBrown600
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    caption: String? = null
) {
    Column(
        modifier = modifier
            .background(StudentWhite, CardShape)
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = StudentBrown600)
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StudentBrown800)
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.labelSmall, color = StudentBrown600)
        }
    }
}

@Composable
private fun GeminiSmartHabitCard(habitTipState: HabitTipState, onFetchTip: () -> Unit, hasData: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudentGoldLight, CardShape)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(StudentGold),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = StudentBlack, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Gemini Smart Habit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Adaptive student coach", style = MaterialTheme.typography.labelSmall, color = StudentBrown600)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxWidth().background(StudentWhite, RoundedCornerShape(12.dp)).padding(14.dp)
        ) {
            when {
                !hasData -> Text(
                    "Log a few expenses and I'll suggest a way to save on your biggest category.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudentBrown600
                )
                habitTipState is HabitTipState.Idle -> Text(
                    "Get a quick, friendly tip based on where most of your money is going.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudentBrown600
                )
                habitTipState is HabitTipState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = StudentGold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Thinking of a tip…", style = MaterialTheme.typography.bodyMedium, color = StudentBrown600)
                }
                habitTipState is HabitTipState.Success -> Text(habitTipState.tip, style = MaterialTheme.typography.bodyMedium, color = StudentBrown800)
                habitTipState is HabitTipState.Error -> Text(habitTipState.message, style = MaterialTheme.typography.bodyMedium, color = StudentBrown600)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onFetchTip,
            enabled = hasData,
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(containerColor = StudentBlack, contentColor = StudentWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Regenerate Tip with Gemini", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SetAllowanceDialog(
    currentAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(if (currentAmount > 0.0) "%.2f".format(currentAmount) else "") }
    val amount = amountText.toDoubleOrNull()

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentAmount > 0.0) "Edit monthly allowance" else "Set your monthly allowance") },
        text = {
            StudentTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = "Amount (R)",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { amount?.let { onConfirm(it) } },
                enabled = amount != null && amount > 0.0
            ) { Text("Save") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
