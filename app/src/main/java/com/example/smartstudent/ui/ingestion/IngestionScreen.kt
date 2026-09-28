package com.example.smartstudent.ui.ingestion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smartstudent.domain.model.DefaultCategoryRules
import com.example.smartstudent.domain.model.TransactionType
import com.example.smartstudent.theme.StudentGray600
import com.example.smartstudent.ui.components.CategoryDropdown
import com.example.smartstudent.ui.components.PrimaryPillButton
import com.example.smartstudent.ui.components.StudentTextField
import com.example.smartstudent.ui.components.TransactionTypeToggle
import com.example.smartstudent.util.formScreen
import com.example.smartstudent.util.parseAmount

/** Manual transaction entry — the "ingestion" flow's simplest path (bulk import lives in the statement scan flow). */
@Composable
fun IngestionScreen(
    knownCategories: List<String>,
    onSave: (title: String, amount: Double, category: String, type: TransactionType) -> Unit,
    initialType: TransactionType = TransactionType.EXPENSE,
    initialCategory: String = ""
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(initialCategory) }
    var type by remember { mutableStateOf(initialType) }
    var categoryTouchedByUser by remember { mutableStateOf(initialCategory.isNotBlank()) }
    var submitted by remember { mutableStateOf(false) }   // guards against a double tap saving twice

    // Auto-suggest a category from the title as the user types, using the local
    // keyword rule set — unless they've already picked/typed their own category.
    LaunchedEffect(title) {
        if (!categoryTouchedByUser) {
            DefaultCategoryRules.suggest(title)?.let { category = it }
        }
    }

    val allCategories = remember(knownCategories) {
        (DefaultCategoryRules.defaultCategories + knownCategories).distinct()
    }

    val parsedAmount = parseAmount(amount)

    Column(modifier = Modifier.formScreen().padding(24.dp)) {
        Text("Add transaction", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Log a purchase or income so we can keep your habits up to date.",
            style = MaterialTheme.typography.bodyMedium,
            color = StudentGray600
        )
        Spacer(modifier = Modifier.height(20.dp))

        TransactionTypeToggle(selected = type, onSelect = { type = it })
        Spacer(modifier = Modifier.height(14.dp))

        StudentTextField(value = title, onValueChange = { title = it }, label = "What was it for?")
        Spacer(modifier = Modifier.height(14.dp))
        StudentTextField(
            value = amount,
            onValueChange = { amount = it },
            label = "Amount (R)",
            keyboardType = KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(14.dp))
        CategoryDropdown(
            value = category,
            onValueChange = {
                category = it
                categoryTouchedByUser = true
            },
            knownCategories = allCategories
        )

        Spacer(modifier = Modifier.height(24.dp))
        PrimaryPillButton(
            text = "Save transaction",
            onClick = {
                parsedAmount?.let {
                    submitted = true
                    onSave(title.trim(), it, category.ifBlank { "Other" }, type)
                }
            },
            enabled = title.isNotBlank() && parsedAmount != null && !submitted,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
