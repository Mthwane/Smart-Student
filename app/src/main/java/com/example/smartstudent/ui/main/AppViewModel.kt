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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

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

    var currentUser by mutableStateOf<User?>(null)
        private set

    var transactions by mutableStateOf<List<Transaction>>(emptyList())
        private set

    var goals by mutableStateOf<List<SavingsGoal>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    /** Short, user-safe message only. See ERROR HANDLING POLICY above. */
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun dismissError() {
        errorMessage = null
    }

    val balance: Double by derivedStateOf {
        transactions.sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
    }

    // --- Analytics ---
    //
    // These all used to be plain `get()` properties that re-scanned the FULL transaction
    // list from scratch every single time anything read them — and Compose re-reads them
    // on every recomposition of Dashboard/Analytics, so a single screen redraw could mean
    // several full passes over transactions. `derivedStateOf` caches the result and only
    // recomputes when `transactions` (or `currentUser`, for allowance) actually changes,
    // which is what makes those screens feel noticeably snappier once there's more than a
    // handful of transactions (e.g. right after a bank statement scan adds a bunch at once).

    private val thisMonthTransactions: List<Transaction> by derivedStateOf {
        val now = LocalDateTime.now()
        transactions.filter { it.date.month == now.month && it.date.year == now.year }
    }

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

    val monthlyAllowance: Double by derivedStateOf {
        currentUser?.monthlyAllowance ?: 0.0
    }

    val allowanceRemaining: Double by derivedStateOf {
        monthlyAllowance - monthlyExpenses
    }

    fun setMonthlyAllowance(amount: Double) {
        val uid = currentUser?.id ?: return
        currentUser = currentUser?.copy(monthlyAllowance = amount)
        viewModelScope.launch {
            try {
                userRepository.setMonthlyAllowance(uid, amount)
            } catch (e: Exception) {
                AppLogger.e("setMonthlyAllowance", e)
                errorMessage = "Couldn't save your allowance. Please try again."
            }
        }
    }

    val isEmailVerified: Boolean
        get() = authRepository.currentUser?.isEmailVerified ?: false

    val pendingVerificationEmail: String
        get() = authRepository.currentUser?.email ?: ""

    fun signUp(firstName: String, lastName: String, email: String, password: String, onDone: (success: Boolean) -> Unit) {
        errorMessage = null
        isLoading = true
        viewModelScope.launch {
            try {
                val firebaseUser = authRepository.signUpWithEmail(email, password)
                userRepository.createProfile(firebaseUser.uid, firstName, lastName, email)
                currentUser = User(id = firebaseUser.uid, firstName = firstName, lastName = lastName, email = email)
                onDone(true)
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
                val firebaseUser = authRepository.signInWithEmail(email, password)
                loadUserProfile(firebaseUser.uid)
                loadData()
                onDone(true)
            } catch (e: Exception) {
                AppLogger.e("logIn", e)
                errorMessage = friendlyAuthMessage(e, fallback = "Couldn't log you in. Check your email and password.")
                onDone(false)
            } finally {
                isLoading = false
            }
        }
    }

    fun googleSignInAvailable(context: Context): Boolean =
        authRepository.buildGoogleSignInClient(context) != null

    fun googleSignInClient(context: Context) = authRepository.buildGoogleSignInClient(context)

    fun completeGoogleSignIn(idToken: String, onDone: (success: Boolean) -> Unit) {
        errorMessage = null
        isLoading = true
        viewModelScope.launch {
            try {
                val firebaseUser = authRepository.signInWithGoogleCredential(idToken)
                val nameParts = (firebaseUser.displayName ?: "").split(" ", limit = 2)
                val first = nameParts.getOrElse(0) { "" }
                val last = nameParts.getOrElse(1) { "" }
                userRepository.createProfile(firebaseUser.uid, first, last, firebaseUser.email ?: "")
                currentUser = User(id = firebaseUser.uid, firstName = first, lastName = last, email = firebaseUser.email ?: "")
                loadData()
                onDone(true)
            } catch (e: Exception) {
                AppLogger.e("completeGoogleSignIn", e)
                errorMessage = "Google sign-in didn't go through. Please try again."
                onDone(false)
            } finally {
                isLoading = false
            }
        }
    }

    /** Signs out and clears all in-memory state so nothing from the previous account lingers. */
    fun logOut() {
        authRepository.signOut()
        currentUser = null
        transactions = emptyList()
        goals = emptyList()
        scanState = StatementScanState.Idle
        habitTipState = HabitTipState.Idle
        errorMessage = null
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
        if (!firebaseUser.isEmailVerified) {
            onResult(AutoLoginResult.LOGGED_IN_UNVERIFIED)
            return
        }
        isLoading = true
        viewModelScope.launch {
            try {
                currentUser = userRepository.getProfile(firebaseUser.uid)
                transactions = transactionRepository.getAll(firebaseUser.uid)
                goals = goalRepository.getAll(firebaseUser.uid)
                onResult(AutoLoginResult.LOGGED_IN_VERIFIED)
            } catch (e: Exception) {
                AppLogger.e("tryAutoLogin", e)
                // Fail safe: if we can't load their data, don't strand them on a broken
                // Dashboard — send them back through a normal login instead.
                onResult(AutoLoginResult.LOGGED_OUT)
            } finally {
                isLoading = false
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            try {
                authRepository.resendVerificationEmail()
            } catch (e: Exception) {
                AppLogger.e("resendVerificationEmail", e)
                errorMessage = "Couldn't resend the email right now. Please try again shortly."
            }
        }
    }

    fun checkEmailVerified(onResult: (verified: Boolean) -> Unit) {
        isLoading = true
        viewModelScope.launch {
            val verified = try {
                authRepository.refreshEmailVerifiedStatus()
            } catch (e: Exception) {
                AppLogger.e("checkEmailVerified", e)
                errorMessage = "Couldn't check verification status. Please try again."
                false
            }
            isLoading = false
            if (verified) loadData()
            onResult(verified)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        val uid = currentUser?.id ?: return
        currentUser = currentUser?.copy(notificationsEnabled = enabled)
        viewModelScope.launch {
            try {
                userRepository.setNotificationsEnabled(uid, enabled)
            } catch (e: Exception) {
                AppLogger.e("setNotificationsEnabled", e)
                errorMessage = "Couldn't save that preference. Please try again."
            }
        }
    }

    fun addTransaction(title: String, amount: Double, category: String, type: TransactionType = TransactionType.EXPENSE) {
        val uid = currentUser?.id ?: return
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            title = title,
            amount = amount,
            type = type,
            category = category,
            date = LocalDateTime.now()
        )
        transactions = transactions + transaction
        viewModelScope.launch {
            try {
                transactionRepository.add(uid, transaction)
            } catch (e: Exception) {
                AppLogger.e("addTransaction", e)
                errorMessage = "Couldn't save that transaction. Please try again."
            }
        }
    }

    /** Edits an existing transaction in place (title/amount/category/type), keeping its id and date. */
    fun updateTransaction(transactionId: String, title: String, amount: Double, category: String, type: TransactionType) {
        val uid = currentUser?.id ?: return
        val existing = transactions.find { it.id == transactionId } ?: return
        val updated = existing.copy(title = title, amount = amount, category = category, type = type)
        transactions = transactions.map { if (it.id == transactionId) updated else it }
        viewModelScope.launch {
            try {
                transactionRepository.update(uid, updated)
            } catch (e: Exception) {
                AppLogger.e("updateTransaction", e)
                errorMessage = "Couldn't save those changes. Please try again."
            }
        }
    }

    fun deleteTransaction(transactionId: String) {
        val uid = currentUser?.id ?: return
        transactions = transactions.filterNot { it.id == transactionId }
        viewModelScope.launch {
            try {
                transactionRepository.delete(uid, transactionId)
            } catch (e: Exception) {
                AppLogger.e("deleteTransaction", e)
                errorMessage = "Couldn't delete that transaction. Please try again."
            }
        }
    }

    fun addGoal(name: String, kind: GoalKind, targetAmount: Double, emoji: String, description: String = "") {
        val uid = currentUser?.id ?: return
        val goal = SavingsGoal(
            id = UUID.randomUUID().toString(),
            name = name,
            kind = kind,
            targetAmount = targetAmount,
            savedAmount = 0.0,
            emoji = emoji,
            description = description
        )
        goals = goals + goal
        viewModelScope.launch {
            try {
                goalRepository.add(uid, goal)
            } catch (e: Exception) {
                AppLogger.e("addGoal", e)
                errorMessage = "Couldn't save that goal. Please try again."
            }
        }
    }

    /** Adds a contribution to an existing goal's saved amount ("Add money"). */
    fun addMoneyToGoal(goalId: String, amount: Double) {
        val uid = currentUser?.id ?: return
        val target = goals.find { it.id == goalId } ?: return
        val updated = target.copy(savedAmount = target.savedAmount + amount)
        goals = goals.map { if (it.id == goalId) updated else it }
        viewModelScope.launch {
            try {
                goalRepository.update(uid, updated)
            } catch (e: Exception) {
                AppLogger.e("addMoneyToGoal", e)
                errorMessage = "Couldn't add that money. Please try again."
            }
        }
    }

    fun deleteGoal(goalId: String) {
        val uid = currentUser?.id ?: return
        goals = goals.filterNot { it.id == goalId }
        viewModelScope.launch {
            try {
                goalRepository.delete(uid, goalId)
            } catch (e: Exception) {
                AppLogger.e("deleteGoal", e)
                errorMessage = "Couldn't delete that goal. Please try again."
            }
        }
    }

    // --- Bank statement scanning (OCR + Gemini) ---

    var scanState by mutableStateOf<StatementScanState>(StatementScanState.Idle)
        private set

    /** Runs on-device OCR on the picked file, then sends the extracted text to Gemini for analysis. */
    fun scanStatement(context: Context, uri: Uri) {
        scanState = StatementScanState.Loading
        viewModelScope.launch {
            try {
                val text = OcrHelper.extractText(context, uri)
                if (text.isBlank()) {
                    AppLogger.w("scanStatement", "OCR returned blank text for uri=$uri")
                    scanState = StatementScanState.Error(
                        "Couldn't read any text from that file. Try a clearer photo or a text-based PDF."
                    )
                    return@launch
                }
                val analysis = geminiRepository.analyzeStatement(
                    statementText = text,
                    currentGoals = goals,
                    monthlyAllowance = monthlyAllowance
                )
                scanState = StatementScanState.Success(analysis)
            } catch (e: Exception) {
                // Full technical detail is always logged (Logcat tag "scanStatement"); a
                // short version is also shown in the UI so a bad API key/model/quota issue
                // is visible without needing to plug into Logcat.
                AppLogger.e("scanStatement", e)
                scanState = StatementScanState.Error(
                    "We couldn't analyze that statement right now (${e.message ?: "unknown error"}). Please try again in a moment."
                )
            }
        }
    }

    fun resetScanState() {
        scanState = StatementScanState.Idle
    }

    /** Imports the parsed transactions the user confirmed from a scan result. */
    fun importScannedTransactions(parsed: List<ParsedTransaction>) {
        val uid = currentUser?.id ?: return
        val newTransactions = parsed.map { p ->
            Transaction(
                id = UUID.randomUUID().toString(),
                title = p.description.ifBlank { "Imported transaction" },
                amount = p.amount,
                type = p.type,
                category = p.category.ifBlank { "Uncategorized" },
                date = parseStatementDate(p.date),
                isRecurring = false
            )
        }
        transactions = transactions + newTransactions
        scanState = StatementScanState.Idle
        viewModelScope.launch {
            newTransactions.forEach { txn ->
                try {
                    transactionRepository.add(uid, txn)
                } catch (e: Exception) {
                    AppLogger.e("importScannedTransactions", e)
                    errorMessage = "Some imported transactions may not have saved. Please check your Activity list."
                }
            }
        }
    }

    private fun parseStatementDate(raw: String): LocalDateTime = try {
        LocalDate.parse(raw).atStartOfDay()
    } catch (e: DateTimeParseException) {
        LocalDateTime.now()
    }

    private fun loadUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                currentUser = userRepository.getProfile(uid) ?: currentUser
            } catch (e: Exception) {
                AppLogger.e("loadUserProfile", e)
                errorMessage = "Couldn't load your profile. Please try again."
            }
        }
    }

    private fun loadData() {
        val uid = currentUser?.id ?: authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                transactions = transactionRepository.getAll(uid)
                goals = goalRepository.getAll(uid)
            } catch (e: Exception) {
                AppLogger.e("loadData", e)
                errorMessage = "Couldn't load your data. Pull to refresh or try again."
            }
        }
    }

    /**
     * Firebase Auth exceptions are already reasonably worded, but can still vary in tone/detail
     * (and rarely leak class names). Map the common cases to a consistent, friendly voice; fall
     * back to a generic message for anything unrecognized rather than showing raw text.
     */
    private fun friendlyAuthMessage(e: Exception, fallback: String): String {
        val raw = e.message ?: return fallback
        return when {
            raw.contains("email address is already in use", ignoreCase = true) ->
                "That email is already registered — try logging in instead."
            raw.contains("badly formatted", ignoreCase = true) ||
                raw.contains("invalid email", ignoreCase = true) ->
                "That doesn't look like a valid email address."
            raw.contains("password is invalid", ignoreCase = true) ||
                raw.contains("no user record", ignoreCase = true) ||
                raw.contains("supplied auth credential is incorrect", ignoreCase = true) ->
                "Incorrect email or password."
            raw.contains("network error", ignoreCase = true) ->
                "Network issue — check your connection and try again."
            raw.contains("too many", ignoreCase = true) ->
                "Too many attempts. Please wait a moment and try again."
            else -> fallback
        }
    }

    // --- Recommended habits (AI spending tip) ---

    var habitTipState by mutableStateOf<HabitTipState>(HabitTipState.Idle)
        private set

    fun fetchHabitTip() {
        if (categorySpendTotals.isEmpty()) return
        habitTipState = HabitTipState.Loading
        viewModelScope.launch {
            try {
                val tip = geminiRepository.suggestSpendingHabitTip(
                    categoryTotals = categorySpendTotals,
                    monthlyIncome = monthlyIncome,
                    monthlyExpenses = monthlyExpenses
                )
                habitTipState = HabitTipState.Success(tip)
            } catch (e: Exception) {
                AppLogger.e("fetchHabitTip", e)
                habitTipState = HabitTipState.Error("Couldn't get a tip right now (${e.message ?: "unknown error"}).")
            }
        }
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
