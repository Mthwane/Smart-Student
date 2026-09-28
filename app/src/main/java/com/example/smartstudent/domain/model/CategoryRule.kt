package com.example.smartstudent.domain.model

/**
 * A rule used to auto-classify an ingested transaction into a category,
 * e.g. merchant name contains "STARBUCKS" -> category "Coffee".
 */
data class CategoryRule(
    val id: String,
    val matchKeyword: String,
    val category: String,
    val priority: Int = 0
) {
    fun matches(merchantOrTitle: String): Boolean =
        merchantOrTitle.contains(matchKeyword, ignoreCase = true)
}
