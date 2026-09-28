package com.example.smartstudent.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.smartstudent.NavigationKeys
import com.example.smartstudent.theme.StudentBeigeBg
import com.example.smartstudent.theme.StudentBlack
import com.example.smartstudent.theme.StudentGold
import com.example.smartstudent.theme.StudentTaupe400

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(NavigationKeys.DASHBOARD, "Home", Icons.Outlined.Home),
    BottomTab(NavigationKeys.TRANSACTIONS, "Activity", Icons.Outlined.Receipt),
    BottomTab(NavigationKeys.GOALS, "Goals", Icons.Outlined.Flag),
    BottomTab(NavigationKeys.ANALYTICS, "Insights", Icons.Outlined.PieChart)
)

/**
 * App shell matching the Figma "SmartStudent" design: a cream pill-tab bottom bar
 * (active tab gets a gold pill behind its icon) plus a persistent gold circular
 * quick-add FAB that floats just above it on every main tab.
 */
@Composable
fun MainScaffold(
    navController: NavHostController,
    onQuickAdd: (() -> Unit)? = null,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = StudentBeigeBg,
        floatingActionButton = {
            if (onQuickAdd != null) {
                FloatingActionButton(
                    onClick = onQuickAdd,
                    containerColor = StudentGold,
                    contentColor = StudentBlack,
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Quick add")
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.background(StudentBeigeBg)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        BottomTabItem(
                            tab = tab,
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(tab.route) {
                                        popUpTo(NavigationKeys.DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        content(padding)
    }
}

@Composable
private fun BottomTabItem(tab: BottomTab, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) StudentGold else androidx.compose.ui.graphics.Color.Transparent)
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Icon(
                tab.icon,
                contentDescription = tab.label,
                tint = StudentBlack,
                modifier = Modifier.height(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            tab.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) StudentBlack else StudentTaupe400
        )
    }
}
