package com.example.smartstudent

/**
 * Centralized route names for the nav graph, kept separate from Navigation.kt
 * so screens can reference route strings without importing the graph itself.
 */
object NavigationKeys {
    const val SPLASH = "splash"

    // Onboarding flow
    const val WELCOME = "onboarding/welcome"
    const val AUTH = "onboarding/auth/{mode}"
    const val EMAIL_VERIFY = "onboarding/email_verify"
    const val ENABLE_NOTIFICATIONS = "onboarding/notifications"

    fun authRoute(mode: String) = "onboarding/auth/$mode"

    // Core app (bottom nav destinations)
    const val DASHBOARD = "main/dashboard"
    const val TRANSACTIONS = "main/transactions"
    const val GOALS = "main/goals"
    const val ANALYTICS = "main/analytics"
    const val INGESTION = "main/ingestion"
    const val STATEMENT_SCAN = "main/statement_scan"

    // Goals sub-flow
    const val GOAL_TYPE_PICKER = "goals/type_picker"
    const val GOAL_CREATE = "goals/create"

    val bottomNavRoutes = listOf(DASHBOARD, TRANSACTIONS, GOALS, ANALYTICS)
}
