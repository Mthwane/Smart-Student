package com.example.smartstudent.ui.transactions

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartstudent.domain.model.Transaction
import com.example.smartstudent.domain.model.TransactionType
import com.example.smartstudent.theme.CardShape
import com.example.smartstudent.theme.InputShape
import com.example.smartstudent.theme.PillShape
import com.example.smartstudent.theme.StudentBeige100
import com.example.smartstudent.theme.StudentBlack
import com.example.smartstudent.theme.StudentBrown600
import com.example.smartstudent.theme.StudentBrown800
import com.example.smartstudent.theme.StudentGold
import com.example.smartstudent.theme.StudentGoldLight
import com.example.smartstudent.theme.StudentGreen
import com.example.smartstudent.theme.StudentWhite
import com.example.smartstudent.ui.components.AppHeader
import com.example.smartstudent.ui.components.CategoryDropdown
import com.example.smartstudent.ui.components.MascotBird
import com.example.smartstudent.ui.components.MascotMood
import com.example.smartstudent.ui.components.StudentTextField
import com.example.smartstudent.ui.components.TransactionTypeToggle
import com.example.smartstudent.ui.components.categoryColorSoft
import com.example.smartstudent.ui.components.categoryIcon
import java.time.format.DateTimeFormatter
import com.example.smartstudent.util.editableAmount
import com.example.smartstudent.util.parseAmount

private sealed class ActivityFilter {
    data object All : ActivityFilter()
    data object Income : ActivityFilter()
    data object Expenses : ActivityFilter()
    data class Category(val name: String) : ActivityFilter()

    val label: String
        get() = when (this) {
            All -> "All"
            Income -> "Income"
            Expenses -> "Expenses"
            is Category -> name
        }
}

// How many category rows are visible at once in the expanded "more categories" panel
// before it scrolls — keeps the panel from growing without bound on accounts with lots
// of distinct categories.
private const val VISIBLE_CATEGORY_ROWS = 4
private val CATEGORY_ROW_HEIGHT = 44.dp

@Composable
fun TransactionsScreen(
    transactions: List<Transaction>,
    knownCategories: List<String>,
    onAddManually: () -> Unit,
    onScanStatement: () -> Unit,
    onUpdateTransaction: (id: String, title: String, amount: Double, category: String, type: TransactionType) -> Unit,
    onDeleteTransaction: (id: String) -> Unit
) {
    var filter by remember { mutableStateOf<ActivityFilter>(ActivityFilter.All) }
    var searchQuery by remember { mutableStateOf("") }
    var transactionBeingEdited by remember { mutableStateOf<Transaction?>(null) }
    var categoriesExpanded by remember { mutableStateOf(false) }

    val categories = remember(transactions) {
        transactions.map { it.category }.distinct().sorted()
    }

    val filtered = remember(transactions, filter, searchQuery) {
        val byFilter = when (val f = filter) {
            is ActivityFilter.All -> transactions
            is ActivityFilter.Income -> transactions.filter { it.type == TransactionType.INCOME }
            is ActivityFilter.Expenses -> transactions.filter { it.type == TransactionType.EXPENSE }
            is ActivityFilter.Category -> transactions.filter { it.category == f.name }
        }
        if (searchQuery.isBlank()) byFilter
        else byFilter.filter {
            it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val grouped = remember(filtered) {
        filtered.sortedByDescending { it.date }.groupBy { it.date.toLocalDate() }
    }

    // Everything — header, search, filters, the OCR banner, and the transaction list —
    // lives in ONE LazyColumn now, so nothing is pinned to the top of the screen.
    // Scroll down and the banner scrolls away with everything else; scroll back up
    // and it reappears, same as any other item in the list.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp)
    ) {
        item {
            AppHeader(title = "Activity Ledger") {
                MascotBird(size = 34.dp, mood = MascotMood.NEUTRAL)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "${transactions.size} Total Movements",
                style = MaterialTheme.typography.bodyMedium,
                color = StudentBrown600
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search payees, categories, tags…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = StudentBrown600) },
                singleLine = true,
                shape = InputShape,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = StudentBeige100,
                    focusedContainerColor = StudentBeige100,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedBorderColor = StudentGold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            PrimaryFilterRow(
                filter = filter,
                transactions = transactions,
                categoriesExpanded = categoriesExpanded,
                hasCategories = categories.isNotEmpty(),
                onSelect = { filter = it },
                onToggleCategories = { categoriesExpanded = !categoriesExpanded }
            )

            if (categoriesExpanded && categories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                CategoryScrollPanel(
                    categories = categories,
                    transactions = transactions,
                    selected = filter,
                    onSelect = {
                        filter = it
                        categoriesExpanded = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            GeminiOcrBanner(onScanStatement = onScanStatement)
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    if (transactions.isEmpty())
                        "No transactions yet — add one manually or scan a statement."
                    else
                        "Nothing in \"${filter.label}\" yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudentBrown600
                )
            }
        }

        grouped.forEach { (date, txns) ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        date.format(DateTimeFormatter.ofPattern("EEEE — d MMM")).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = StudentBrown600,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${txns.size} transaction${if (txns.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = StudentBrown600
                    )
                }
            }
            items(txns, key = { it.id }) { txn ->
                TransactionListRow(txn, onClick = { transactionBeingEdited = txn })
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    transactionBeingEdited?.let { txn ->
        EditTransactionDialog(
            transaction = txn,
            knownCategories = (com.example.smartstudent.domain.model.DefaultCategoryRules.defaultCategories + knownCategories).distinct(),
            onDismiss = { transactionBeingEdited = null },
            onSave = { title, amount, category, type ->
                onUpdateTransaction(txn.id, title, amount, category, type)
                transactionBeingEdited = null
            },
            onDelete = {
                onDeleteTransaction(txn.id)
                transactionBeingEdited = null
            }
        )
    }
}

@Composable
private fun PrimaryFilterRow(
    filter: ActivityFilter,
    transactions: List<Transaction>,
    categoriesExpanded: Boolean,
    hasCategories: Boolean,
    onSelect: (ActivityFilter) -> Unit,
    onToggleCategories: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActivityFilterChip(
            label = "All",
            count = transactions.size,
            selected = filter is ActivityFilter.All,
            onClick = { onSelect(ActivityFilter.All) }
        )
        ActivityFilterChip(
            label = "Income",
            count = transactions.count { it.type == TransactionType.INCOME },
            selected = filter is ActivityFilter.Income,
            onClick = { onSelect(ActivityFilter.Income) }
        )
        ActivityFilterChip(
            label = "Expenses",
            count = transactions.count { it.type == TransactionType.EXPENSE },
            selected = filter is ActivityFilter.Expenses,
            onClick = { onSelect(ActivityFilter.Expenses) }
        )
        if (hasCategories) {
            MoreCategoriesChip(
                expanded = categoriesExpanded,
                selected = filter is ActivityFilter.Category,
                onClick = onToggleCategories
            )
        }
    }
}

@Composable
private fun MoreCategoriesChip(expanded: Boolean, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(if (selected) StudentBlack else StudentBeige100)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "More",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) StudentWhite else StudentBrown800
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) "Collapse categories" else "More categories",
            tint = if (selected) StudentWhite else StudentBrown800,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Extra categories beyond All/Income/Expenses. Capped to [VISIBLE_CATEGORY_ROWS] rows
 * tall and vertically scrollable past that — so an account with dozens of categories
 * gets a short scrollable panel here instead of the filter row growing indefinitely.
 */
@Composable
private fun CategoryScrollPanel(
    categories: List<String>,
    transactions: List<Transaction>,
    selected: ActivityFilter,
    onSelect: (ActivityFilter) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = CATEGORY_ROW_HEIGHT * VISIBLE_CATEGORY_ROWS)
            .background(StudentBeige100, RoundedCornerShape(14.dp))
            .verticalScroll(scrollState)
            .padding(vertical = 4.dp)
    ) {
        categories.forEach { category ->
            val isSelected = selected is ActivityFilter.Category && selected.name == category
            val count = transactions.count { it.category == category }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CATEGORY_ROW_HEIGHT)
                    .clickable { onSelect(ActivityFilter.Category(category)) }
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) StudentGold else StudentBrown800
                )
                Text("$count", style = MaterialTheme.typography.labelSmall, color = StudentBrown600)
            }
        }
    }
}

@Composable
private fun ActivityFilterChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(if (selected) StudentBlack else StudentBeige100)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) StudentWhite else StudentBrown800
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "$count",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) StudentGold else StudentBrown600
        )
    }
}

@Composable
private fun GeminiOcrBanner(onScanStatement: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudentBlack, CardShape)
            .clickable(onClick = onScanStatement)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(StudentGold),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.DocumentScanner, contentDescription = null, tint = StudentBlack)
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.clip(PillShape).background(StudentGold).padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("GEMINI AI", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = StudentBlack)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bank Statement OCR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StudentWhite)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Upload statements or snap a receipt. Gemini auto-classifies your spending with 98% accuracy.",
                style = MaterialTheme.typography.bodyMedium,
                color = StudentTaupeOnBlack
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onScanStatement,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(containerColor = StudentGold, contentColor = StudentBlack),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text("Scan Statement", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private val StudentTaupeOnBlack = androidx.compose.ui.graphics.Color(0xFFBFBBB0)

@Composable
private fun EditTransactionDialog(
    transaction: Transaction,
    knownCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, category: String, type: TransactionType) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(transaction.title) }
    var amountText by remember { mutableStateOf(editableAmount(transaction.amount)) }
    var category by remember { mutableStateOf(transaction.category) }
    var type by remember { mutableStateOf(transaction.type) }
    val amount = parseAmount(amountText)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit transaction") },
        text = {
            Column {
                TransactionTypeToggle(selected = type, onSelect = { type = it })
                Spacer(modifier = Modifier.height(12.dp))
                StudentTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "What was it for?"
                )
                Spacer(modifier = Modifier.height(12.dp))
                StudentTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Amount (R)",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
                Spacer(modifier = Modifier.height(12.dp))
                CategoryDropdown(
                    value = category,
                    onValueChange = { category = it },
                    knownCategories = knownCategories
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.TextButton(onClick = onDelete) {
                    Text("Delete transaction", color = com.example.smartstudent.theme.StudentRed)
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { amount?.let { onSave(title, it, category.ifBlank { "Other" }, type) } },
                enabled = title.isNotBlank() && amount != null
            ) { Text("Save") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun TransactionListRow(transaction: Transaction, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudentWhite, CardShape)
            .clickable(onClick = onClick)
            .padding(14.dp),
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
                Text(
                    transaction.category + " • " + transaction.date.format(DateTimeFormatter.ofPattern("h:mm a")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudentBrown600
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                (if (transaction.type == TransactionType.INCOME) "+" else "-") + "R%,.2f".format(transaction.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == TransactionType.INCOME) StudentGreen else StudentBrown800
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier.clip(PillShape).background(StudentGoldLight).padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(transaction.category, style = MaterialTheme.typography.labelSmall, color = StudentBrown800)
            }
        }
    }
}
