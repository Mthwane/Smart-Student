package com.example.smartstudent.domain.model

/**
 * A small built-in rule set so manual transaction entry can auto-suggest a category
 * from the title as the user types, without needing a Gemini call for every keystroke
 * (Gemini is reserved for full statement scans). Statement-scanned transactions get
 * their categories from Gemini directly; this is the lightweight local fallback.
 */
object DefaultCategoryRules {

    val rules: List<CategoryRule> = listOf(
        CategoryRule("r1", "coffee", "Coffee"),
        CategoryRule("r2", "cafe", "Coffee"),
        CategoryRule("r3", "starbucks", "Coffee"),
        CategoryRule("r4", "uber", "Transport"),
        CategoryRule("r5", "bolt", "Transport"),
        CategoryRule("r6", "taxi", "Transport"),
        CategoryRule("r7", "gautrain", "Transport"),
        CategoryRule("r8", "rent", "Rent"),
        CategoryRule("r9", "digs", "Rent"),
        CategoryRule("r10", "grocer", "Groceries"),
        CategoryRule("r11", "checkers", "Groceries"),
        CategoryRule("r12", "woolworths", "Groceries"),
        CategoryRule("r13", "pick n pay", "Groceries"),
        CategoryRule("r14", "spar", "Groceries"),
        CategoryRule("r15", "netflix", "Entertainment"),
        CategoryRule("r16", "showmax", "Entertainment"),
        CategoryRule("r17", "spotify", "Entertainment"),
        CategoryRule("r18", "cinema", "Entertainment"),
        CategoryRule("r19", "movie", "Entertainment"),
        CategoryRule("r20", "clicks", "Cosmetics"),
        CategoryRule("r21", "dischem", "Cosmetics"),
        CategoryRule("r22", "pharmacy", "Cosmetics"),
        CategoryRule("r23", "salary", "Income"),
        CategoryRule("r24", "wage", "Income"),
        CategoryRule("r25", "bursary", "Income"),
        CategoryRule("r26", "nsfas", "Income"),
        CategoryRule("r27", "tutoring", "Income"),
        CategoryRule("r28", "stipend", "Income"),
        CategoryRule("r29", "electric", "Utilities"),
        CategoryRule("r30", "airtime", "Utilities"),
        CategoryRule("r31", "data bundle", "Utilities"),
        CategoryRule("r32", "wifi", "Utilities")
    )

    /** The default set of categories offered even before the user has any transactions. */
    val defaultCategories: List<String> = listOf(
        "Groceries", "Coffee", "Transport", "Rent", "Entertainment",
        "Cosmetics", "Utilities", "Income", "Transfer", "Other"
    )

    /** Best-guess category for a transaction title, or null if nothing matches. */
    fun suggest(title: String): String? =
        rules.sortedByDescending { it.priority }.firstOrNull { it.matches(title) }?.category
}
