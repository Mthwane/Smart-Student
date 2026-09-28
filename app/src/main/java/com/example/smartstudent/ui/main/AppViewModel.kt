package com.example.smartstudent.ui.main

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartstudent.data.ocr.OcrHelper
import com.example.smartstudent.data.repository.AuthRepository
import com.example.smartstudent.data.repository.GeminiRepository
import com.example.smartstudent.data.repository.GoalRepository
import com.example.smartstudent.data.repository.TransactionRepository
import com.example.smartstudent.data.repository.UserRepository
import com.example.smartstudent.domain.model.GoalKind
import com.example.smartstudent.domain.model.ParsedTransaction
import com.example.smartstudent.domain.model.SavingsGoal
import com.example.smartstudent.domain.model.StatementAnalysis
import com.example.smartstudent.domain.model.Transaction
import com.example.smartstudent.domain.model.TransactionType
import com.example.smartstudent.domain.model.User
import com.example.smartstudent.util.AppLogger
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/**
 * Shared app state backed by real Firebase repositories (Auth + Firestore).
 * Screens read state via these mutableStateOf properties and trigger loads/writes
 * through the public functions below.
 *
 * ERROR HANDLING POLICY: every catch block here logs the real exception via
 * AppLogger (visible in Logcat) and exposes only a short, generic, user-safe
 * message to the UI. Raw exception text, stack traces, and API error payloads
 * (e.g. Gemini's JSON error body) must never reach errorMessage or scanState.
 */
class AppViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val transactionRepository: TransactionRepository = TransactionRepository(),
    private val goalRepository: GoalRepository = GoalRepository(),
    private val geminiRepository: GeminiRepository = GeminiRepository()
) : ViewModel() {

    var currentUser by mutableStateOf<User?>(null); private set
    var transactions by mutableStateOf<List<Transaction>>(emptyList()); private set
    var goals by mutableStateOf<List<SavingsGoal>>(emptyList()); private set
    var isLoading by mutableStateOf(false); private set

    /** Short, user-safe message only. See ERROR HANDLING POLICY above. */
    var errorMessage by mutableStateOf<String?>(null); private set

    /** Short, user-safe informational message (e.g. "Password reset email sent."). */
    var infoMessage by mutableStateOf<String?>(null); private set

    fun dismissError() { errorMessage = null }
    fun dismissInfo() { infoMessage = null }

    /** Lets the UI layer surface an error it detected itself (e.g. a cancelled/failed Google sign-in). */
    fun reportError(message: String) { errorMessage = message }

    /** Always the live Firebase uid, so writes never silently no-op just because the profile hasn't loaded yet. */
    private val uid: String? get() = authRepository.currentUser?.uid

    // --- clock, so "this month" rolls over correctly if the app is left open across midnight on the 1st ---
    private var today by mutableStateOf(LocalDate.now())
    fun refreshClock() { val now = LocalDate.now(); if (now != today) today = now }

    val balance: Double by derivedStateOf {
        transactions.sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
    }

    // --- Analytics ---
    //
    // These use derivedStateOf so they're cached and only recomputed when `transactions`,
    // `currentUser`, or `today` actually change, rather than re-scanning the full list on
    // every recomposition of Dashboard/Analytics.

    private val thisMonthTransactions: List<Transaction> by derivedStateOf {
        val month = YearMonth.from(today)
        transactions.filter { YearMonth.from(it.date) == month }
    }

    val monthlyTransactionCount: Int by derivedStateOf { thisMonthTransactions.size }

    val monthlyIncome: Double by derivedStateOf {
        thisMonthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }

    val monthlyExpenses: Double by derivedStateOf {
        thisMonthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    /** Income:expense ratio, e.g. 1.0 means spending exactly matches income. Null if no income yet. */
    val incomeToExpenseRatio: Double? by derivedStateOf {
        if (monthlyIncome <= 0.0) null else monthlyExpenses / monthlyIncome
    }

    val averageExpenseAmount: Double by derivedStateOf {
        val expenses = thisMonthTransactions.filter { it.type == TransactionType.EXPENSE }
        if (expenses.isEmpty()) 0.0 else expenses.sumOf { it.amount } / expenses.size
    }

    val categorySpendTotals: Map<String, Double> by derivedStateOf {
        thisMonthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
    }

    val monthlyAllowance: Double by derivedStateOf { currentUser?.monthlyAllowance ?: 0.0 }
    val allowanceRemaining: Double by derivedStateOf { monthlyAllowance - monthlyExpenses }

    // --- helpers ---

    private inline fun launchCatching(
        tag: String,
        userMessage: String,
        crossinline onFailure: () -> Unit = {},
        crossinline block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(tag, e)
                onFailure()
                errorMessage = userMessage
            }
        }
    }

    private fun splitName(name: String?): Pair<String, String> {
        val parts = (name ?: "").trim().split(" ", limit = 2)
        return parts.getOrElse(0) { "" } to parts.getOrElse(1) { "" }
    }

    private fun fallbackUser(firebaseUser: FirebaseUser): User {
        val (first, last) = splitName(firebaseUser.displayName)
        return User(id = firebaseUser.uid, firstName = first, lastName = last, email = firebaseUser.email.orEmpty())
    }

    /** Loads profile + transactions + goals together and AWAITS it, so callers never move on with stale/empty data. */
    private suspend fun loadAllSafely(firebaseUser: FirebaseUser) {
        val (first, last) = splitName(firebaseUser.displayName)
        try {
            coroutineScope {
                val profileDeferred = async {
                    userRepository.getOrCreateProfile(firebaseUser.uid, first, last, firebaseUser.email.orEmpty())
                }
                val transactionsDeferred = async { transactionRepository.getAll(firebaseUser.uid) }
                val goalsDeferred = async { goalRepository.getAll(firebaseUser.uid) }
                currentUser = profileDeferred.await()
                transactions = transactionsDeferred.await()
                goals = goalsDeferred.await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e("loadAll", e)
            if (currentUser?.id != firebaseUser.uid) currentUser = fallbackUser(firebaseUser)
            errorMessage = "Couldn't load your data. Check your connection and try again."
        }
    }

    /** Re-syncs from the server. Used as the rollback path after a multi-write operation partially fails. */
    fun reload() {
        val firebaseUser = authRepository.currentUser ?: return
        viewModelScope.launch { loadAllSafely(firebaseUser) }
    }

    // --- auth ---

    val isEmailVerified: Boolean get() = authRepository.currentUser?.isEmailVerified ?: false
    val pendingVerificationEmail: String get() = authRepository.currentUser?.email ?: ""

    fun signUp(firstName: String, lastName: String, email: String, password: String, onDone: (success: Boolean) -> Unit) {
        errorMessage = null
        isLoading = true
        viewModelScope.launch {
            try {
                val firebaseUser = authRepository.signUpWithEmail(email.trim(), password)
                // Optimistic: the auth account now exists either way, so let the user proceed to
                // email verification even if the profile write below fails; it's recreated on next login.
                currentUser = User(id = firebaseUser.uid, firstName = firstName, lastName = lastName, email = email.trim())
                try {
                    userRepository.getOrCreateProfile(firebaseUser.uid, firstName, lastName, email.trim())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppLogger.e("signUp/profile", e)
                }
                onDone(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e("signUp", e)
                errorMessage = friendlyAuthMessage(e, fallback = "Couldn't create your account. Please try again.")
                onDone(false)
            } finally {
                isLoading = false
            }
        }
    }

    fun logIn(email: String, password: String, onDone: (success: Boolean) -> Unit) {
        errorMessage = null
        isLoading = true
        viewModelScope.launch {
            try {
                val firebaseUser = authRepository.signInWithEmail(email.trim(), password)
                loadAllSafely(firebaseUser)   // awaited: the dashboard never shows empty/R0.00 first
                onDone(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e("logIn", e)
                errorMessage = friendlyAuthMessage(e, fallback = "Couldn't log you in. Check your email and password.")
                onDone(false)
            } finally {
                isLoading = false
            }
        }
    }

    fun googleSignInAvailable(context: Context): Boolean = authRepository.buildGoogleSignInClient(context) != null
    fun googleSignInClient(context: Context) = authRepository.buildGoogleSignInClient(context)

    fun completeGoogleSignIn(idToken: String, onDone: (success: Boolean) -> Unit) {
        errorMessage = null
        isLoading = true
        viewModelScope.launch {
            try {
                val firebaseUser = authRepository.signInWithGoogleCredential(idToken)
                loadAllSafely(firebaseUser)   // get-or-create: an existing profile's allowance/prefs survive
                onDone(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e("completeGoogleSignIn", e)
                errorMessage = "Google sign-in didn't go through. Please try again."
                onDone(false)
            } finally {
                isLoading = false
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (!email.contains("@")) {
            errorMessage = "Enter your email address first."
            return
        }
        launchCatching("passwordReset", "Couldn't send the reset email. Please try again.") {
            authRepository.sendPasswordReset(email)
            infoMessage = "If an account exists for that email, a reset link is on its way."
        }
    }

    /** Signs out and clears all in-memory state so nothing from the previous account lingers. */
    fun logOut(context: Context) {
        scanJob?.cancel()
        tipJob?.cancel()
        authRepository.signOut(context.applicationContext)
        currentUser = null
        transactions = emptyList()
        goals = emptyList()
        scanState = StatementScanState.Idle
        habitTipState = HabitTipState.Idle
        errorMessage = null
        infoMessage = null
    }

    /**
     * Checks whether Firebase already has a signed-in user (it persists sessions across
     * app restarts on its own) and, if so, loads their profile/data so the app can skip
     * straight past Welcome/login. Called once from [com.example.smartstudent.ui.onboarding.SplashScreen].
     */
    fun tryAutoLogin(onResult: (AutoLoginResult) -> Unit) {
        val firebaseUser = authRepository.currentUser
        if (firebaseUser == null) {
            onResult(AutoLoginResult.LOGGED_OUT)
            return
        }
        isLoading = true
        viewModelScope.launch {
            try {
                val verified = try {
                    authRepository.refreshEmailVerifiedStatus()
                } catch (e: FirebaseAuthInvalidUserException) {
                    // Account deleted/disabled server-side — the local session is stale.
                    authRepository.signOut()
                    onResult(AutoLoginResult.LOGGED_OUT)
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppLogger.e("autoLogin/reload", e)
                    firebaseUser.isEmailVerified   // offline: fall back to the last-known flag
                }

                if (!verified) {
                    currentUser = fallbackUser(firebaseUser)
                    onResult(AutoLoginResult.LOGGED_IN_UNVERIFIED)
                } else {
                    loadAllSafely(firebaseUser)   // offline/failure still lands on the dashboard, with a message
                    onResult(AutoLoginResult.LOGGED_IN_VERIFIED)
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun resendVerificationEmail(onResult: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.resendVerificationEmail()
                onResult(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e("resendVerificationEmail", e)
                onResult(false)
            }
        }
    }

    fun checkEmailVerified(onResult: (verified: Boolean) -> Unit) {
        isLoading = true
        viewModelScope.launch {
            val verified = try {
                authRepository.refreshEmailVerifiedStatus()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e("checkEmailVerified", e)
                errorMessage = "Couldn't check verification status. Please try again."
                false
            }
            // Previously this only re-loaded transactions/goals and left currentUser null,
            // so every write made right after verifying (uid ?: return) silently did nothing.
            if (verified) authRepository.currentUser?.let { loadAllSafely(it) }
            isLoading = false
            onResult(verified)
        }
    }

    // --- profile ---

    fun setNotificationsEnabled(enabled: Boolean) {
        val u = uid ?: return
        val previous = currentUser?.notificationsEnabled ?: false
        currentUser = currentUser?.copy(notificationsEnabled = enabled)
        launchCatching(
            "setNotificationsEnabled", "Couldn't save that preference. Please try again.",
            onFailure = { currentUser = currentUser?.copy(notificationsEnabled = previous) }
        ) {
            userRepository.setNotificationsEnabled(u, enabled)
        }
    }

    fun setMonthlyAllowance(amount: Double) {
        val u = uid ?: return
        if (!amount.isFinite() || amount <= 0.0) {
            errorMessage = "Enter an amount greater than zero."
            return
        }
        val previous = currentUser?.monthlyAllowance ?: 0.0
        currentUser = currentUser?.copy(monthlyAllowance = amount)
        launchCatching(
            "setMonthlyAllowance", "Couldn't save your allowance. Please try again.",
            onFailure = { currentUser = currentUser?.copy(monthlyAllowance = previous) }
        ) {
            userRepository.setMonthlyAllowance(u, amount)
        }
    }

    // --- transactions ---

    fun addTransaction(title: String, amount: Double, category: String, type: TransactionType = TransactionType.EXPENSE) {
        val u = uid ?: return
        if (!amount.isFinite() || amount <= 0.0) {
            errorMessage = "Enter an amount greater than zero."
            return
        }
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            amount = amount,
            type = type,
            category = category.ifBlank { "Other" },
            date = LocalDateTime.now()
        )
        transactions = transactions + transaction
        launchCatching(
            "addTransaction", "Couldn't save that transaction. Please try again.",
            onFailure = { transactions = transactions.filterNot { it.id == transaction.id } }
        ) {
            transactionRepository.add(u, transaction)
        }
    }

    fun updateTransaction(id: String, title: String, amount: Double, category: String, type: TransactionType) {
        val u = uid ?: return
        if (!amount.isFinite() || amount <= 0.0) {
            errorMessage = "Enter an amount greater than zero."
            return
        }
        val existing = transactions.find { it.id == id } ?: return
        val updated = existing.copy(title = title.trim(), amount = amount, category = category, type = type)
        transactions = transactions.map { if (it.id == id) updated else it }
        launchCatching(
            "updateTransaction", "Couldn't save those changes. Please try again.",
            onFailure = { transactions = transactions.map { if (it.id == id) existing else it } }
        ) {
            transactionRepository.update(u, updated)
        }
    }

    fun deleteTransaction(id: String) {
        val u = uid ?: return
        val existing = transactions.find { it.id == id } ?: return
        transactions = transactions.filterNot { it.id == id }
        launchCatching(
            "deleteTransaction", "Couldn't delete that transaction. Please try again.",
            onFailure = { transactions = transactions + existing }
        ) {
            transactionRepository.delete(u, id)
        }
    }

    // --- goals ---

    fun addGoal(name: String, kind: GoalKind, targetAmount: Double, emoji: String, description: String = "", dueDate: LocalDate? = null) {
        val u = uid ?: return
        if (!targetAmount.isFinite() || targetAmount <= 0.0) {
            errorMessage = "Enter a target greater than zero."
            return
        }
        val goal = SavingsGoal(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            kind = kind,
            targetAmount = targetAmount,
            savedAmount = 0.0,
            dueDate = dueDate,
            emoji = emoji,
            description = description.trim()
        )
        goals = goals + goal
        launchCatching(
            "addGoal", "Couldn't save that goal. Please try again.",
            onFailure = { goals = goals.filterNot { it.id == goal.id } }
        ) {
            goalRepository.add(u, goal)
        }
    }

    /** Adds to a goal AND records the matching "Savings" expense so the balance reflects the money set aside. */
    fun addMoneyToGoal(goalId: String, amount: Double) {
        val u = uid ?: return
        if (!amount.isFinite() || amount <= 0.0) {
            errorMessage = "Enter an amount greater than zero."
            return
        }
        val target = goals.find { it.id == goalId } ?: return
        val updatedGoal = target.copy(savedAmount = target.savedAmount + amount)
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            title = "Saved for ${target.name}",
            amount = amount,
            type = TransactionType.EXPENSE,
            category = "Savings",
            date = LocalDateTime.now()
        )
        goals = goals.map { if (it.id == goalId) updatedGoal else it }
        transactions = transactions + transaction
        launchCatching(
            "addMoneyToGoal", "Couldn't add that money. Please try again.",
            onFailure = { reload() }   // two writes involved: resync rather than guess what landed
        ) {
            goalRepository.update(u, updatedGoal)
            transactionRepository.add(u, transaction)
        }
    }

    fun deleteGoal(goalId: String) {
        val u = uid ?: return
        val existing = goals.find { it.id == goalId } ?: return
        goals = goals.filterNot { it.id == goalId }
        launchCatching(
            "deleteGoal", "Couldn't delete that goal. Please try again.",
            onFailure = { goals = goals + existing }
        ) {
            goalRepository.delete(u, goalId)
        }
    }

    // --- statement scanning ---

    var scanState by mutableStateOf<StatementScanState>(StatementScanState.Idle); private set
    private var scanJob: Job? = null

    fun scanStatement(context: Context, uri: Uri) {
        if (scanState is StatementScanState.Loading) return
        scanState = StatementScanState.Loading
        val app = context.applicationContext   // never hold an Activity reference in a ViewModel coroutine
        scanJob = viewModelScope.launch {
            try {
                val text = OcrHelper.extractText(app, uri)
                if (text.isBlank()) {
                    scanState = StatementScanState.Error(
                        "Couldn't read any text from that file. Try a clearer photo or a text-based PDF."
                    )
                    return@launch
                }
                val analysis = geminiRepository.analyzeStatement(text, goals, monthlyAllowance)
                scanState = StatementScanState.Success(analysis)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e("scanStatement", e)
                scanState = StatementScanState.Error(
                    if (e is IOException) "Check your internet connection and try again."
                    else "We couldn't analyze that statement right now. Please try again in a moment."
                )
            }
        }
    }

    fun cancelScan() { scanJob?.cancel(); scanState = StatementScanState.Idle }
    fun resetScanState() { scanState = StatementScanState.Idle }

    private fun dedupeKey(date: LocalDate, amount: Double, title: String) =
        "$date|${String.format(Locale.US, "%.2f", amount)}|${title.trim().lowercase()}"

    fun importScannedTransactions(parsed: List<ParsedTransaction>) {
        val u = uid ?: return
        val existingKeys = transactions.map { dedupeKey(it.date.toLocalDate(), it.amount, it.title) }.toHashSet()

        val candidates = parsed.mapNotNull { p ->
            val amt = abs(p.amount)
            if (!amt.isFinite() || amt == 0.0) return@mapNotNull null
            Transaction(
                id = UUID.randomUUID().toString(),
                title = p.description.ifBlank { "Imported transaction" }.trim(),
                amount = amt,
                type = p.type,
                category = p.category.ifBlank { "Uncategorized" },
                date = parseStatementDate(p.date)
            )
        }
        val fresh = candidates.filter { dedupeKey(it.date.toLocalDate(), it.amount, it.title) !in existingKeys }
        val skipped = parsed.size - fresh.size

        scanState = StatementScanState.Idle
        if (fresh.isEmpty()) {
            infoMessage = "Those transactions are already in your Activity list."
            return
        }
        if (skipped > 0) infoMessage = "Imported ${fresh.size}; skipped $skipped duplicate or invalid."

        transactions = transactions + fresh
        val importedIds = fresh.map { it.id }.toSet()
        launchCatching(
            "importScanned", "The import didn't save. Please try again.",
            onFailure = { transactions = transactions.filterNot { it.id in importedIds } }
        ) {
            transactionRepository.addAll(u, fresh)   // atomic per-batch write
        }
    }

    private fun parseStatementDate(raw: String): LocalDateTime = try {
        LocalDate.parse(raw.trim()).atStartOfDay()
    } catch (e: DateTimeParseException) {
        LocalDateTime.now()
    }

    // --- AI habit tip ---

    var habitTipState by mutableStateOf<HabitTipState>(HabitTipState.Idle); private set
    private var tipJob: Job? = null

    fun fetchHabitTip() {
        if (categorySpendTotals.isEmpty() || habitTipState is HabitTipState.Loading) return
        habitTipState = HabitTipState.Loading
        tipJob = viewModelScope.launch {
            try {
                val tip = geminiRepository.suggestSpendingHabitTip(
                    categoryTotals = categorySpendTotals,
                    monthlyIncome = monthlyIncome,
                    monthlyExpenses = monthlyExpenses
                )
                habitTipState = HabitTipState.Success(tip)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e("fetchHabitTip", e)
                habitTipState = HabitTipState.Error("Couldn't get a tip right now. Please try again.")
            }
        }
    }

    fun dismissHabitTip() { tipJob?.cancel(); habitTipState = HabitTipState.Idle }

    private fun friendlyAuthMessage(e: Exception, fallback: String): String = when (e) {
        is FirebaseNetworkException -> "Network issue — check your connection and try again."
        is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a moment and try again."
        is FirebaseAuthException -> when (e.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "That email is already registered — try logging in instead."
            "ERROR_INVALID_EMAIL" -> "That doesn't look like a valid email address."
            "ERROR_WEAK_PASSWORD" -> "Choose a stronger password (at least 8 characters)."
            "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL" -> "Incorrect email or password."
            "ERROR_USER_DISABLED" -> "This account has been disabled."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait a moment and try again."
            else -> fallback
        }
        else -> fallback
    }
}

sealed class StatementScanState {
    data object Idle : StatementScanState()
    data object Loading : StatementScanState()
    data class Success(val analysis: StatementAnalysis) : StatementScanState()
    data class Error(val message: String) : StatementScanState()
}

sealed class HabitTipState {
    data object Idle : HabitTipState()
    data object Loading : HabitTipState()
    data class Success(val tip: String) : HabitTipState()
    data class Error(val message: String) : HabitTipState()
}

enum class AutoLoginResult { LOGGED_OUT, LOGGED_IN_UNVERIFIED, LOGGED_IN_VERIFIED }
