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
    private val regex: Regex by lazy {
        val kw = Regex.escape(matchKeyword.trim())
        // Word must START at a boundary; short keywords must also END at one.
        val end = if (matchKeyword.trim().length <= 4) "(?![A-Za-z0-9])" else ""
        Regex("(?<![A-Za-z0-9])$kw$end", RegexOption.IGNORE_CASE)
    }

    fun matches(text: String): Boolean = regex.containsMatchIn(text)
}
