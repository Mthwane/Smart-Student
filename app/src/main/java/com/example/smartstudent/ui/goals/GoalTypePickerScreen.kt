package com.example.smartstudent.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartstudent.domain.model.GoalKind
import com.example.smartstudent.theme.CategoryAmberBadge
import com.example.smartstudent.theme.StudentBlue
import com.example.smartstudent.theme.StudentGray600
import com.example.smartstudent.ui.components.IconBadge
import com.example.smartstudent.ui.components.OutlinedRowCard
import com.example.smartstudent.ui.components.TextLinkButton

@Composable
fun GoalTypePickerScreen(
    onClose: () -> Unit,
    onPick: (GoalKind) -> Unit,
    onLearnAboutSmartBills: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = onClose, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close")
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text("What do you want to save for?", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "We'll automatically set aside money to help you reach your goals.",
                style = MaterialTheme.typography.bodyLarge,
                color = StudentGray600
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedRowCard(
                title = "Savings goal",
                subtitle = "Save a little every day. Great for a trip, a gift, or an emergency fund. It all adds up.",
                leading = {
                    IconBadge(backgroundColor = CategoryAmberBadge) {
                        Icon(Icons.Filled.Savings, contentDescription = null)
                    }
                },
                onClick = { onPick(GoalKind.SAVINGS_GOAL) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedRowCard(
                title = "Smart bill",
                subtitle = "Set a repeating goal that saves weekly & sends your money back before the due date of your bill, rent or utilities.",
                leading = {
                    IconBadge(backgroundColor = StudentBlue.copy(alpha = 0.15f)) {
                        Icon(Icons.Filled.Receipt, contentDescription = null, tint = StudentBlue)
                    }
                },
                onClick = { onPick(GoalKind.SMART_BILL) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            TextLinkButton(text = "Learn about Smart bills", onClick = onLearnAboutSmartBills)
        }
    }
}
