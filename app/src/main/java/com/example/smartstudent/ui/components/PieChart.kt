package com.example.smartstudent.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smartstudent.theme.CategoryChartPalette
import com.example.smartstudent.theme.StudentGray600

data class PieSlice(val label: String, val value: Double, val color: Color)

/** Builds slices from raw category totals, auto-assigning colors from the earth-tone palette. */
fun buildPieSlices(categoryTotals: Map<String, Double>): List<PieSlice> {
    val sorted = categoryTotals.entries.sortedByDescending { it.value }
    val top = sorted.take(5)
    val rest = sorted.drop(5).sumOf { it.value }
    // Combine anything past the top 5 into "Other" so the ring and the legend always agree.
    val entries = top.map { it.key to it.value } + if (rest > 0) listOf("Other" to rest) else emptyList()
    return entries.mapIndexed { index, (label, value) ->
        PieSlice(label, value, CategoryChartPalette[index % CategoryChartPalette.size])
    }
}

/**
 * Centered donut chart with a legend underneath (rather than side-by-side, which on
 * mobile widths tends to push the donut against the left edge). The whole block is
 * horizontally centered as a unit within whatever width its parent gives it.
 */
@Composable
fun CategoryPieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.value }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            if (total <= 0.0) return@Canvas
            var startAngle = -90f
            val strokeWidthPx = size.minDimension * 0.22f
            // Inset the arc's bounding box by half the stroke width, or the ring clips at the canvas edge.
            val arcTopLeft = androidx.compose.ui.geometry.Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)
            val arcSize = androidx.compose.ui.geometry.Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
            slices.forEach { slice ->
                val sweep = (slice.value / total * 360.0).toFloat()
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx),
                )
                startAngle += sweep
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(horizontalAlignment = Alignment.Start) {
            slices.forEach { slice ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(slice.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(slice.label, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(6.dp))
                    val pct = if (total > 0) (slice.value / total * 100).toInt() else 0
                    Text("$pct%", style = MaterialTheme.typography.labelMedium, color = StudentGray600)
                }
            }
        }
    }
}
