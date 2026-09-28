package com.example.smartstudent

import android.Manifest
import android.app.Activity.RESULT_OK
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartstudent.domain.model.DefaultCategoryRules
import com.example.smartstudent.domain.model.GoalKind
import com.example.smartstudent.domain.model.TransactionType
import com.example.smartstudent.ui.analytics.AnalyticsScreen
import com.example.smartstudent.ui.dashboard.DashboardScreen
import com.example.smartstudent.ui.goals.GoalCreateScreen
import com.example.smartstudent.ui.goals.GoalTypePickerScreen
import com.example.smartstudent.ui.goals.GoalsScreen
import com.example.smartstudent.ui.ingestion.IngestionScreen
import com.example.smartstudent.ui.ingestion.StatementScanScreen
import com.example.smartstudent.ui.main.AppViewModel
import com.example.smartstudent.ui.main.AppViewModelFactory
import com.example.smartstudent.ui.main.AutoLoginResult
import com.example.smartstudent.ui.main.MainScaffold
import com.example.smartstudent.ui.main.StatementScanState
import com.example.smartstudent.ui.onboarding.AuthMode
import com.example.smartstudent.ui.onboarding.AuthScreen
import com.example.smartstudent.ui.onboarding.EmailVerificationScreen
import com.example.smartstudent.ui.onboarding.EnableNotificationsScreen
import com.example.smartstudent.ui.onboarding.SplashScreen
import com.example.smartstudent.ui.onboarding.WelcomeScreen
import com.example.smartstudent.ui.transactions.TransactionsScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay

private const val GOOGLE_SIGN_IN_CANCELLED = 12501

@Composable
fun SmartStudentNavHost() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = viewModel(factory = AppViewModelFactory())
    val context = LocalContext.current
    var pendingGoalKind by rememberSaveable { mutableStateOf(GoalKind.SAVINGS_GOAL) }
    val snackbarHostState = remember { SnackbarHostState() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val onAuthRoute = backStackEntry?.destination?.route?.startsWith("onboarding/auth") == true

    // Errors used to only render on AuthScreen, so failures everywhere else were invisible.
    LaunchedEffect(appViewModel.errorMessage, onAuthRoute) {
        val msg = appViewModel.errorMessage
        if (msg != null && !onAuthRoute) {
            snackbarHostState.showSnackbar(msg)
            appViewModel.dismissError()
        }
    }
    LaunchedEffect(appViewModel.infoMessage) {
        appViewModel.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            appViewModel.dismissInfo()
        }
    }
    // Keeps "this month" correct if the app stays open across a midnight-on-the-1st rollover.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            appViewModel.refreshClock()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = NavigationKeys.SPLASH) {

            composable(NavigationKeys.SPLASH) {
                LaunchedEffect(Unit) {
                    appViewModel.tryAutoLogin { result ->
                        val target = when (result) {
                            AutoLoginResult.LOGGED_IN_VERIFIED -> NavigationKeys.DASHBOARD
                            AutoLoginResult.LOGGED_IN_UNVERIFIED -> NavigationKeys.EMAIL_VERIFY
                            AutoLoginResult.LOGGED_OUT -> NavigationKeys.WELCOME
                        }
                        navController.navigate(target) { popUpTo(NavigationKeys.SPLASH) { inclusive = true } }
                    }
                }
                SplashScreen()
            }

            composable(NavigationKeys.WELCOME) {
                WelcomeScreen(
                    onLogin = { navController.navigate(NavigationKeys.authRoute("login")) },
                    onSignUp = { navController.navigate(NavigationKeys.authRoute("signup")) }
                )
            }

            composable(
                NavigationKeys.AUTH,
                arguments = listOf(navArgument("mode") { type = NavType.StringType })
            ) { backStackEntry ->
                val modeArg = backStackEntry.arguments?.getString("mode") ?: "signup"

                // Don't let an error from a previous screen linger here on first composition.
                DisposableEffect(Unit) {
                    appViewModel.dismissError()
                    onDispose { appViewModel.dismissError() }
                }

                val googleLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        try {
                            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                                .getResult(ApiException::class.java)
                            val idToken = account?.idToken
                            if (idToken != null) {
                                appViewModel.completeGoogleSignIn(idToken) { success ->
                                    if (success) navController.navigateToMainGraph()
                                }
                            } else {
                                appViewModel.reportError("Google sign-in didn't return a token. Please try again.")
                            }
                        } catch (e: ApiException) {
                            if (e.statusCode != GOOGLE_SIGN_IN_CANCELLED) {
                                appViewModel.reportError("Google sign-in didn't go through. Please try again.")
                            }
                        }
                    }
                }

                AuthScreen(
                    initialMode = if (modeArg == "login") AuthMode.LOG_IN else AuthMode.SIGN_UP,
                    onBack = { navController.popBackStack() },
                    onSignUp = { details ->
                        appViewModel.signUp(
                            details.firstName.trim(), details.lastName.trim(), details.email, details.password
                        ) { success -> if (success) navController.navigate(NavigationKeys.EMAIL_VERIFY) }
                    },
                    onLogIn = { email, password ->
                        appViewModel.logIn(email, password) { success ->
                            if (success) {
                                if (appViewModel.isEmailVerified) navController.navigateToMainGraph()
                                else navController.navigate(NavigationKeys.EMAIL_VERIFY)
                            }
                        }
                    },
                    onForgotPassword = { email -> appViewModel.sendPasswordReset(email) },
                    onGoogleSignIn = {
                        appViewModel.googleSignInClient(context)?.let { googleLauncher.launch(it.signInIntent) }
                    },
                    googleSignInAvailable = appViewModel.googleSignInAvailable(context),
                    loading = appViewModel.isLoading,
                    errorMessage = appViewModel.errorMessage,
                    infoMessage = appViewModel.infoMessage
                )
            }

            composable(NavigationKeys.EMAIL_VERIFY) {
                var checking by remember { mutableStateOf(false) }
                var info by remember { mutableStateOf<String?>(null) }
                var cooldown by remember { mutableIntStateOf(0) }
                LaunchedEffect(cooldown) {
                    if (cooldown > 0) {
                        delay(1000)
                        cooldown -= 1
                    }
                }

                EmailVerificationScreen(
                    email = appViewModel.pendingVerificationEmail,
                    onResend = {
                        if (cooldown == 0) {
                            appViewModel.resendVerificationEmail { success ->
                                if (success) {
                                    info = "Verification email sent."
                                    cooldown = 30
                                } else {
                                    info = "Couldn't resend the email right now. Please try again shortly."
                                }
                            }
                        }
                    },
                    resendCooldown = cooldown,
                    onSignOut = {
                        appViewModel.logOut(context)
                        navController.navigateToWelcomeGraph()
                    },
                    onIveVerified = {
                        checking = true
                        appViewModel.checkEmailVerified { verified ->
                            checking = false
                            if (verified) {
                                navController.navigate(NavigationKeys.ENABLE_NOTIFICATIONS)
                            } else {
                                info = "Still not verified — check your inbox (and spam folder) and try again."
                            }
                        }
                    },
                    checking = checking,
                    infoMessage = info
                )
            }

            composable(NavigationKeys.ENABLE_NOTIFICATIONS) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    appViewModel.setNotificationsEnabled(granted)
                    navController.navigateToMainGraph()
                }
                EnableNotificationsScreen(
                    onNotNow = { navController.navigateToMainGraph() },
                    onEnable = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            appViewModel.setNotificationsEnabled(true)
                            navController.navigateToMainGraph()
                        }
                    }
                )
            }

            composable(NavigationKeys.DASHBOARD) {
                MainScaffold(navController, onQuickAdd = { navController.navigate(NavigationKeys.INGESTION) }) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        DashboardScreen(
                            studentFirstName = appViewModel.currentUser?.firstName?.ifBlank { null } ?: "there",
                            balance = appViewModel.balance,
                            monthlyAllowance = appViewModel.monthlyAllowance,
                            allowanceRemaining = appViewModel.allowanceRemaining,
                            monthlyExpenses = appViewModel.monthlyExpenses,
                            recentTransactions = appViewModel.transactions.sortedByDescending { it.date }.take(5),
                            habitTipState = appViewModel.habitTipState,
                            onFetchHabitTip = { appViewModel.fetchHabitTip() },
                            onDismissHabitTip = { appViewModel.dismissHabitTip() },
                            onAddMoney = { navController.navigate(NavigationKeys.ingestion("INCOME")) },
                            onTransfer = { navController.navigate(NavigationKeys.ingestion("EXPENSE", "Transfer")) },
                            onSetBudget = { navController.navigate(NavigationKeys.ANALYTICS) },
                            onScanStatement = { navController.navigate(NavigationKeys.STATEMENT_SCAN) },
                            onAddManually = { navController.navigate(NavigationKeys.INGESTION) },
                            onSeeAllActivity = { navController.navigate(NavigationKeys.TRANSACTIONS) },
                            onLogout = {
                                appViewModel.logOut(context)
                                navController.navigateToWelcomeGraph()
                            }
                        )
                    }
                }
            }

            composable(NavigationKeys.TRANSACTIONS) {
                MainScaffold(navController, onQuickAdd = { navController.navigate(NavigationKeys.INGESTION) }) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        TransactionsScreen(
                            transactions = appViewModel.transactions,
                            knownCategories = appViewModel.transactions.map { it.category }.distinct(),
                            onAddManually = { navController.navigate(NavigationKeys.INGESTION) },
                            onScanStatement = { navController.navigate(NavigationKeys.STATEMENT_SCAN) },
                            onUpdateTransaction = { id, title, amount, category, type ->
                                appViewModel.updateTransaction(id, title, amount, category, type)
                            },
                            onDeleteTransaction = { id -> appViewModel.deleteTransaction(id) }
                        )
                    }
                }
            }

            composable(NavigationKeys.GOALS) {
                MainScaffold(navController, onQuickAdd = { navController.navigate(NavigationKeys.GOAL_TYPE_PICKER) }) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        GoalsScreen(
                            goals = appViewModel.goals,
                            onAddGoal = { navController.navigate(NavigationKeys.GOAL_TYPE_PICKER) },
                            onPickKind = { kind ->
                                pendingGoalKind = kind
                                navController.navigate(NavigationKeys.GOAL_CREATE)
                            },
                            onAddMoney = { goalId, amount -> appViewModel.addMoneyToGoal(goalId, amount) },
                            onDeleteGoal = { goalId -> appViewModel.deleteGoal(goalId) }
                        )
                    }
                }
            }

            composable(NavigationKeys.ANALYTICS) {
                MainScaffold(navController, onQuickAdd = { navController.navigate(NavigationKeys.INGESTION) }) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        AnalyticsScreen(
                            monthlyIncome = appViewModel.monthlyIncome,
                            monthlyExpenses = appViewModel.monthlyExpenses,
                            averageExpenseAmount = appViewModel.averageExpenseAmount,
                            monthlyTransactionCount = appViewModel.monthlyTransactionCount,
                            categorySpendTotals = appViewModel.categorySpendTotals,
                            monthlyAllowance = appViewModel.monthlyAllowance,
                            allowanceRemaining = appViewModel.allowanceRemaining,
                            habitTipState = appViewModel.habitTipState,
                            onFetchHabitTip = { appViewModel.fetchHabitTip() },
                            onSetAllowance = { amount -> appViewModel.setMonthlyAllowance(amount) }
                        )
                    }
                }
            }

            composable(NavigationKeys.GOAL_TYPE_PICKER) {
                GoalTypePickerScreen(
                    onClose = { navController.popBackStack() },
                    onPick = { kind ->
                        pendingGoalKind = kind
                        navController.navigate(NavigationKeys.GOAL_CREATE)
                    }
                )
            }

            composable(NavigationKeys.GOAL_CREATE) {
                GoalCreateScreen(
                    kind = pendingGoalKind,
                    onSave = { name, targetAmount, description, emoji, dueDate ->
                        appViewModel.addGoal(
                            name = name,
                            kind = pendingGoalKind,
                            targetAmount = targetAmount,
                            emoji = emoji,
                            description = description,
                            dueDate = dueDate
                        )
                        if (!navController.popBackStack(NavigationKeys.GOALS, inclusive = false)) {
                            navController.navigate(NavigationKeys.GOALS) { popUpTo(NavigationKeys.DASHBOARD) }
                        }
                    }
                )
            }

            composable(
                NavigationKeys.INGESTION_ROUTE,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType; defaultValue = "EXPENSE" },
                    navArgument("category") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                val initialType = runCatching {
                    TransactionType.valueOf(entry.arguments?.getString("type") ?: "EXPENSE")
                }.getOrDefault(TransactionType.EXPENSE)
                val initialCategory = entry.arguments?.getString("category").orEmpty()
                val knownCategories = remember(appViewModel.transactions) {
                    (DefaultCategoryRules.defaultCategories + appViewModel.transactions.map { it.category }).distinct()
                }
                IngestionScreen(
                    knownCategories = knownCategories,
                    initialType = initialType,
                    initialCategory = initialCategory,
                    onSave = { title, amount, category, type ->
                        appViewModel.addTransaction(title, amount, category, type)
                        navController.popBackStack()
                    }
                )
            }

            composable(NavigationKeys.STATEMENT_SCAN) {
                val filePickerLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri -> if (uri != null) appViewModel.scanStatement(context, uri) }

                BackHandler(enabled = appViewModel.scanState is StatementScanState.Loading) {
                    appViewModel.cancelScan()
                }
                DisposableEffect(Unit) {
                    onDispose {
                        if (appViewModel.scanState !is StatementScanState.Loading) appViewModel.resetScanState()
                    }
                }

                StatementScanScreen(
                    scanState = appViewModel.scanState,
                    onPickFile = { filePickerLauncher.launch(arrayOf("image/*", "application/pdf")) },
                    onImport = { parsedTransactions ->
                        appViewModel.importScannedTransactions(parsedTransactions)
                        navController.popBackStack()
                    },
                    onRetry = { appViewModel.resetScanState() }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp)
        )
    }
}

private fun NavHostController.navigateToMainGraph() {
    navigate(NavigationKeys.DASHBOARD) { popUpTo(graph.id) { inclusive = true } }
}

private fun NavHostController.navigateToWelcomeGraph() {
    navigate(NavigationKeys.WELCOME) { popUpTo(graph.id) { inclusive = true } }
}
