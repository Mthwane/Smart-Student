package com.example.smartstudent.util

import java.util.Locale

/** Accepts "1500.50", "1 500,50", "1,500.50". Returns null for blank, <= 0, NaN or Infinity. */
fun parseAmount(input: String): Double? {
    val s = input.trim().replace(" ", "").replace("\u00A0", "")
    val normalised = if (s.contains(',') && s.contains('.')) s.replace(",", "") else s.replace(',', '.')
    return normalised.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
}

/** Text to prefill into an amount field: always a dot, always parseable by [parseAmount]. */
fun editableAmount(amount: Double): String = String.format(Locale.US, "%.2f", amount)
