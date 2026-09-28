package com.example.smartstudent.ui.ingestion

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartstudent.domain.model.BudgetStatus
import com.example.smartstudent.domain.model.GoalInsight
import com.example.smartstudent.domain.model.ParsedTransaction
import com.example.smartstudent.domain.model.StatementAnalysis
import com.example.smartstudent.domain.model.TransactionType
import com.example.smartstudent.theme.CardShape
import com.example.smartstudent.theme.StudentGray600
import com.example.smartstudent.theme.StudentGreen
import com.example.smartstudent.theme.StudentGreenLight
import com.example.smartstudent.theme.StudentRed
import com.example.smartstudent.theme.StudentRedLight
import com.example.smartstudent.theme.StudentGoldLight
import com.example.smartstudent.ui.components.MascotBird
import com.example.smartstudent.ui.components.MascotMood
import com.example.smartstudent.ui.components.PrimaryPillButton
import com.example.smartstudent.ui.components.ResultOutcome
import com.example.smartstudent.ui.components.ResultSplash
import com.example.smartstudent.ui.main.StatementScanState
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.mutableStateListOf

@Composable
fun StatementScanScreen(
    scanState: StatementScanState,
    onPickFile: () -> Unit,
    onImport: (List<ParsedTransaction>) -> Unit,
    onRetry: () -> Unit
) {
    // Failure gets a full-screen animated splash, not raw error text inline.
    if (scanState is StatementScanState.Error) {
        ResultSplash(
            outcome = ResultOutcome.FAILURE,
            title = "Scan didn't work out",
            message = scanState.message,
            actionLabel = "Try again",
            onAction = onRetry,
            applySystemBars = true
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        Text("Scan bank statement", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Upload a photo or PDF of a statement. Text is read on your device; the extracted text " +
                "(with long account/card numbers removed) is then sent to Google's Gemini to categorize it.",
            style = MaterialTheme.typography.bodyMedium,
            color = StudentGray600
        )
        Spacer(modifier = Modifier.height(24.dp))

        when (scanState) {
            is StatementScanState.Idle -> IdleState(onPickFile)
            is StatementScanState.Loading -> LoadingState()
            is StatementScanState.Success -> SuccessThenResults(scanState, onImport)
            is StatementScanState.Error -> Unit // handled above with the full-screen splash
        }
    }
}

/** Shows a brief animated success celebration, then reveals the results list. */
@Composable
private fun SuccessThenResults(
    state: StatementScanState.Success,
    onImport: (List<ParsedTransaction>) -> Unit
) {
    var celebrating by remember(state) { mutableStateOf(true) }

    if (celebrating) {
        ResultSplash(
            outcome = ResultOutcome.SUCCESS,
            title = "Statement analyzed!",
            message = "Found ${state.analysis.transactions.size} transaction${if (state.analysis.transactions.size == 1) "" else "s"} — take a look below.",
            modifier = Modifier.fillMaxSize()
        )
        LaunchedEffect(state) {
            delay(1400)
            celebrating = false
        }
    } else {
        ResultState(state.analysis, onImport)
    }
}

@Composable
private fun IdleState(onPickFile: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        MascotBird(size = 96.dp, mood = MascotMood.NEUTRAL)
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryPillButton(text = "Choose file", onClick = onPickFile, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = StudentGray600)
            Spacer(modifier = Modifier.height(0.dp))
            Text("  Accepts images (JPG/PNG) and PDF statements", style = MaterialTheme.typography.labelMedium, color = StudentGray600)
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        MascotBird(size = 96.dp, mood = MascotMood.NEUTRAL)
        Spacer(modifier = Modifier.height(20.dp))
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Reading and analyzing your statement…", style = MaterialTheme.typography.bodyLarge)
        Text(
            "This can take up to a minute for longer statements.",
            style = MaterialTheme.typography.bodyMedium,
            color = StudentGray600
        )
    }
}

@Composable
private fun ResultState(analysis: StatementAnalysis, onImport: (List<ParsedTransaction>) -> Unit) {
    val selected = remember(analysis) { mutableStateListOf(*Array(analysis.transactions.size) { true }) }
    val chosen = analysis.transactions.filterIndexed { index, _ -> selected.getOrElse(index) { true } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            BudgetStatusBanner(analysis.budgetStatus, analysis.summary)
        }

        if (analysis.overspendingCategories.isNotEmpty()) {
            item {
                Column {
                    Text("Overspending in", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        analysis.overspendingCategories.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudentRed
                    )
                }
            }
        }

        if (analysis.goalInsights.isNotEmpty()) {
            item {
                Column {
                    Text("Goal check-in", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    analysis.goalInsights.forEach { insight -> GoalInsightRow(insight) }
                }
            }
        }

        item {
            Text(
                "Found ${analysis.transactions.size} transaction${if (analysis.transactions.size == 1) "" else "s"}",
                style = MaterialTheme.typography.titleMedium
            )
        }

        itemsIndexed(analysis.transactions) { index, txn ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = selected.getOrElse(index) { true },
                    onCheckedChange = { checked -> if (index < selected.size) selected[index] = checked }
                )
                Box(modifier = Modifier.weight(1f)) { ParsedTransactionRow(txn) }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            PrimaryPillButton(
                text = "Import ${chosen.size} transaction${if (chosen.size == 1) "" else "s"}",
                onClick = { onImport(chosen) },
                enabled = chosen.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BudgetStatusBanner(status: BudgetStatus, summary: String) {
    val (bg, label) = when (status) {
        BudgetStatus.OVER_BUDGET -> StudentRedLight to "Over budget"
        BudgetStatus.UNDER_BUDGET -> StudentGreenLight to "Under budget"
        BudgetStatus.ON_TRACK -> StudentGreenLight to "On track"
        BudgetStatus.UNKNOWN -> StudentGoldLight to "Budget status unclear"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, CardShape)
            .padding(16.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (summary.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun GoalInsightRow(insight: GoalInsight) {
    val statusColor = when (insight.status.lowercase()) {
        "achieved", "on_track" -> StudentGreen
        "behind" -> StudentRed
        else -> StudentGray600
    }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(insight.goalName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(insight.status.replace("_", " "), style = MaterialTheme.typography.labelMedium, color = statusColor)
        }
        if (insight.note.isNotBlank()) {
            Text(insight.note, style = MaterialTheme.typography.bodyMedium, color = StudentGray600)
        }
    }
}

@Composable
private fun ParsedTransactionRow(txn: ParsedTransaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, CardShape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(txn.description, style = MaterialTheme.typography.titleMedium)
            Text("${txn.category} · ${txn.date}", style = MaterialTheme.typography.bodyMedium, color = StudentGray600)
        }
        Text(
            (if (txn.type == TransactionType.INCOME) "+" else "-") + "R%.2f".format(kotlin.math.abs(txn.amount)),
            style = MaterialTheme.typography.titleMedium,
            color = if (txn.type == TransactionType.INCOME) StudentGreen else MaterialTheme.colorScheme.onSurface
        )
    }
}
