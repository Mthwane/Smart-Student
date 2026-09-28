package com.example.smartstudent.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.smartstudent.theme.CategoryChartPalette
import com.example.smartstudent.theme.StudentGreen
import androidx.compose.material.icons.filled.Savings

/** Picks a friendly icon for a free-text transaction category, matched loosely by keyword. */
fun categoryIcon(category: String): ImageVector {
    val c = category.lowercase()
    return when {
        "saving" in c -> Icons.Filled.Savings
        "coffee" in c || "cafe" in c || "dining" in c || "food" in c -> Icons.Filled.LocalCafe
        "book" in c || "education" in c || "tuition" in c || "school" in c -> Icons.Filled.MenuBook
        "transit" in c || "transport" in c || "uber" in c || "taxi" in c || "bus" in c -> Icons.Filled.DirectionsBus
        "grocery" in c || "groceries" in c || "market" in c || "supplies" in c -> Icons.Filled.ShoppingBasket
        "entertainment" in c || "spotify" in c || "movie" in c || "stream" in c -> Icons.Filled.MovieFilter
        "income" in c || "stipend" in c || "salary" in c || "tutoring" in c -> Icons.Filled.School
        "transfer" in c -> Icons.Filled.SwapHoriz
        "invest" in c -> Icons.Filled.TrendingUp
        "snack" in c || "restaurant" in c -> Icons.Filled.Fastfood
        else -> Icons.Filled.Receipt
    }
}

/** Deterministic accent color per category, drawn from the earth-tone chart palette. */
fun categoryColor(category: String): Color {
    if (category.isBlank()) return CategoryChartPalette.first()
    val index = kotlin.math.abs(category.lowercase().hashCode()) % CategoryChartPalette.size
    return CategoryChartPalette[index]
}

/** Soft tint (20% alpha) of the category color, used as an icon-badge background. */
fun categoryColorSoft(category: String): Color = categoryColor(category).copy(alpha = 0.16f)

val IncomeAccentColor: Color get() = StudentGreen
