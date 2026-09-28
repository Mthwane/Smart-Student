package com.example.smartstudent.ui.dashboard

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartstudent.domain.model.Transaction
import com.example.smartstudent.domain.model.TransactionType
import com.example.smartstudent.theme.CardShape
import com.example.smartstudent.theme.PillShape
import com.example.smartstudent.theme.StudentBeige100
import com.example.smartstudent.theme.StudentBlack
import com.example.smartstudent.theme.StudentBrown600
import com.example.smartstudent.theme.StudentBrown800
import com.example.smartstudent.theme.StudentCardBlack
import com.example.smartstudent.theme.StudentGold
import com.example.smartstudent.theme.StudentGoldLight
import com.example.smartstudent.theme.StudentGreen
import com.example.smartstudent.theme.StudentRed
import com.example.smartstudent.theme.StudentWhite
import com.example.smartstudent.ui.components.AppHeader
import com.example.smartstudent.ui.components.MascotBird
import com.example.smartstudent.ui.components.MascotMood
import com.example.smartstudent.ui.components.categoryColorSoft
import com.example.smartstudent.ui.components.categoryIcon
import com.example.smartstudent.ui.main.HabitTipState

@Composable
fun DashboardScreen(
    studentFirstName: String,
    balance: Double,
    monthlyAllowance: Double,
    allowanceRemaining: Double,
    monthlyExpenses: Double,
    recentTransactions: List<Transaction>,
    habitTipState: HabitTipState,
    onFetchHabitTip: () -> Unit,
    onAddMoney: () -> Unit,
    onTransfer: () -> Unit,
    onSetBudget: () -> Unit,
    onScanStatement: () -> Unit,
    onAddManually: () -> Unit,
    onSeeAllActivity: () -> Unit,
    onLogout: () -> Unit
) {
    val overBudget = monthlyAllowance > 0.0 && allowanceRemaining < 0
    val spentFraction = if (monthlyAllowance > 0.0) (monthlyExpenses / monthlyAllowance).toFloat().coerceIn(0f, 1f) else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            AppHeader(title = "Dashboard", onSettingsClick = {}) {
                MascotBird(size = 34.dp, mood = MascotMood.HAPPY, onTap = onLogout)
            }
        }

        item {
            StudentPulseBanner(studentFirstName = studentFirstName, onLogout = onLogout)
        }

        item {
            TotalBalanceCard(
                balance = balance,
                monthlyAllowance = monthlyAllowance,
                allowanceRemaining = allowanceRemaining,
                monthlyExpenses = monthlyExpenses,
                spentFraction = spentFraction,
                overBudget = overBudget,
                onAddMoney = onAddMoney,
                onTransfer = onTransfer,
                onSetBudget = onSetBudget
            )
        }

        item {
            GeminiSmartHabitCard(
                habitTipState = habitTipState,
                onFetchTip = onFetchHabitTip
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(StudentGold)
                    )
                }
                Text(
                    "See all ›",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudentGold,
                    modifier = Modifier.clickable(onClick = onSeeAllActivity)
                )
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Text(
                    "No activity yet — scan a receipt or add one manually below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudentBrown600
                )
            }
        }

        items(recentTransactions, key = { it.id }) { txn ->
            DashboardTransactionRow(txn)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onScanStatement,
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = StudentGold, contentColor = StudentBlack),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Filled.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Receipt", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onAddManually,
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = StudentBlack, contentColor = StudentWhite),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manual", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StudentPulseBanner(studentFirstName: String, onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudentBeige100, CardShape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MascotBird(size = 40.dp, mood = MascotMood.HAPPY)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "STUDENT PULSE",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudentGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StudentGreen))
            }
            Text(
                "Hey $studentFirstName, you're on track!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.Logout,
            contentDescription = "Log out",
            tint = StudentBrown600,
            modifier = Modifier.size(20.dp).clickable(onClick = onLogout)
        )
    }
}

@Composable
private fun TotalBalanceCard(
    balance: Double,
    monthlyAllowance: Double,
    allowanceRemaining: Double,
    monthlyExpenses: Double,
    spentFraction: Float,
    overBudget: Boolean,
    onAddMoney: () -> Unit,
    onTransfer: () -> Unit,
    onSetBudget: () -> Unit
) {
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
            Text("TOTAL BALANCE", style = MaterialTheme.typography.labelMedium, color = StudentTaupeOnBlack)
            Row(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), PillShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = StudentGold, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Live", style = MaterialTheme.typography.labelSmall, color = StudentGold)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "R%,.2f".format(balance),
            style = MaterialTheme.typography.displayLarge,
            color = StudentWhite
        )

        if (monthlyAllowance > 0.0) {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Monthly Allowance", style = MaterialTheme.typography.bodyMedium, color = StudentTaupeOnBlack)
                Text(
                    if (overBudget) "R%,.2f over".format(-allowanceRemaining) else "R%,.2f left".format(allowanceRemaining),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (overBudget) StudentRed else StudentGold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { spentFraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = if (overBudget) StudentRed else StudentGold,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("R%,.2f spent".format(monthlyExpenses), style = MaterialTheme.typography.labelMedium, color = StudentTaupeOnBlack)
                Text("R%,.2f cap".format(monthlyAllowance), style = MaterialTheme.typography.labelMedium, color = StudentTaupeOnBlack)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAddMoney,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(containerColor = StudentGold, contentColor = StudentBlack),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onTransfer,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = StudentWhite),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Transfer", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onSetBudget,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = StudentWhite),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Budget", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private val StudentTaupeOnBlack = Color(0xFFBFBBB0)

@Composable
private fun GeminiSmartHabitCard(habitTipState: HabitTipState, onFetchTip: () -> Unit) {
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
            Text("GEMINI SMART HABIT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = StudentBrown800)
        }
        Spacer(modifier = Modifier.height(10.dp))

        when (habitTipState) {
            is HabitTipState.Idle -> {
                Text("Top category insight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Get a quick, personalized tip based on where most of your money is going this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudentBrown600
                )
                Spacer(modifier = Modifier.height(12.dp))
                HabitCardButtons(primaryLabel = "Try This Habit", onPrimary = onFetchTip)
            }
            is HabitTipState.Loading -> {
                Text("Thinking of a tip…", style = MaterialTheme.typography.bodyMedium, color = StudentBrown600)
            }
            is HabitTipState.Success -> {
                Text(habitTipState.tip, style = MaterialTheme.typography.bodyMedium, color = StudentBrown800)
                Spacer(modifier = Modifier.height(12.dp))
                HabitCardButtons(primaryLabel = "Try This Habit", onPrimary = onFetchTip, secondaryLabel = "Dismiss")
            }
            is HabitTipState.Error -> {
                Text(habitTipState.message, style = MaterialTheme.typography.bodyMedium, color = StudentBrown600)
                Spacer(modifier = Modifier.height(12.dp))
                HabitCardButtons(primaryLabel = "Try Again", onPrimary = onFetchTip)
            }
        }
    }
}

@Composable
private fun HabitCardButtons(primaryLabel: String, onPrimary: () -> Unit, secondaryLabel: String? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onPrimary,
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(containerColor = StudentBlack, contentColor = StudentWhite),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(primaryLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        if (secondaryLabel != null) {
            Button(
                onClick = {},
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.5f), contentColor = StudentBrown800),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(secondaryLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DashboardTransactionRow(transaction: Transaction) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(categoryColorSoft(transaction.category)),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryIcon(transaction.category), contentDescription = null, tint = StudentBrown800, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(transaction.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(transaction.category, style = MaterialTheme.typography.bodyMedium, color = StudentBrown600)
            }
        }
        Text(
            (if (transaction.type == TransactionType.INCOME) "+" else "-") + "R%,.2f".format(transaction.amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (transaction.type == TransactionType.INCOME) StudentGreen else StudentBrown800
        )
    }
}
