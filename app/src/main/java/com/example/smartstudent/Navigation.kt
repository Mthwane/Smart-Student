package com.example.smartstudent

import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartstudent.domain.model.DefaultCategoryRules
import com.example.smartstudent.domain.model.GoalKind
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
import com.example.smartstudent.ui.onboarding.AuthMode
import com.example.smartstudent.ui.onboarding.AuthScreen
import com.example.smartstudent.ui.onboarding.EmailVerificationScreen
import com.example.smartstudent.ui.onboarding.EnableNotificationsScreen
import com.example.smartstudent.ui.onboarding.SplashScreen
import com.example.smartstudent.ui.onboarding.WelcomeScreen
import com.example.smartstudent.ui.transactions.TransactionsScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun SmartStudentNavHost() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = viewModel(factory = AppViewModelFactory())
    var pendingGoalKind by remember { mutableStateOf(GoalKind.SAVINGS_GOAL) }

    NavHost(navController = navController, startDestination = NavigationKeys.SPLASH) {

        composable(NavigationKeys.SPLASH) {
            LaunchedEffect(Unit) {
                appViewModel.tryAutoLogin { result ->
                    when (result) {
                        AutoLoginResult.LOGGED_IN_VERIFIED -> navController.navigate(NavigationKeys.DASHBOARD) {
                            popUpTo(NavigationKeys.SPLASH) { inclusive = true }
                        }
                        AutoLoginResult.LOGGED_IN_UNVERIFIED -> navController.navigate(NavigationKeys.EMAIL_VERIFY) {
                            popUpTo(NavigationKeys.SPLASH) { inclusive = true }
                        }
                        AutoLoginResult.LOGGED_OUT -> navController.navigate(NavigationKeys.WELCOME) {
                            popUpTo(NavigationKeys.SPLASH) { inclusive = true }
                        }
                    }
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
            val context = LocalContext.current

            val googleLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        val idToken = account?.idToken
                        if (idToken != null) {
                            appViewModel.completeGoogleSignIn(idToken) { success ->
                                if (success) navController.navigateToMainGraph()
                            }
                        }
                    } catch (_: ApiException) {
                        // Sign-in cancelled or failed silently; user can retry.
                    }
                }
            }

            AuthScreen(
                initialMode = if (modeArg == "login") AuthMode.LOG_IN else AuthMode.SIGN_UP,
                onBack = { navController.popBackStack() },
                onSignUp = { details ->
                    appViewModel.signUp(details.firstName, details.lastName, details.email, details.password) { success ->
                        if (success) navController.navigate(NavigationKeys.EMAIL_VERIFY)
                    }
                },
                onLogIn = { email, password ->
                    appViewModel.logIn(email, password) { success ->
                        if (success) {
                            if (appViewModel.isEmailVerified) {
                                navController.navigateToMainGraph()
                            } else {
                                navController.navigate(NavigationKeys.EMAIL_VERIFY)
                            }
                        }
                    }
                },
                onGoogleSignIn = {
                    val client = appViewModel.googleSignInClient(context)
                    if (client != null) {
                        googleLauncher.launch(client.signInIntent)
                    }
                },
                googleSignInAvailable = appViewModel.googleSignInAvailable(context),
                loading = appViewModel.isLoading,
                errorMessage = appViewModel.errorMessage
            )
        }

        composable(NavigationKeys.EMAIL_VERIFY) {
            var checking by remember { mutableStateOf(false) }
            var info by remember { mutableStateOf<String?>(null) }

            EmailVerificationScreen(
                email = appViewModel.pendingVerificationEmail,
                onResend = {
                    appViewModel.resendVerificationEmail()
                    info = "Verification email resent."
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
            EnableNotificationsScreen(
                onNotNow = { navController.navigateToMainGraph() },
                onEnable = {
                    appViewModel.setNotificationsEnabled(true)
                    navController.navigateToMainGraph()
                }
            )
        }

        composable(NavigationKeys.DASHBOARD) {
            MainScaffold(navController, onQuickAdd = { navController.navigate(NavigationKeys.INGESTION) }) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    DashboardScreen(
                        studentFirstName = appViewModel.currentUser?.firstName ?: "there",
                        balance = appViewModel.balance,
                        monthlyAllowance = appViewModel.monthlyAllowance,
                        allowanceRemaining = appViewModel.allowanceRemaining,
                        monthlyExpenses = appViewModel.monthlyExpenses,
                        recentTransactions = appViewModel.transactions.sortedByDescending { it.date }.take(5),
                        habitTipState = appViewModel.habitTipState,
                        onFetchHabitTip = { appViewModel.fetchHabitTip() },
                        onAddMoney = { navController.navigate(NavigationKeys.INGESTION) },
                        onTransfer = { navController.navigate(NavigationKeys.INGESTION) },
                        onSetBudget = { navController.navigate(NavigationKeys.ANALYTICS) },
                        onScanStatement = { navController.navigate(NavigationKeys.STATEMENT_SCAN) },
                        onAddManually = { navController.navigate(NavigationKeys.INGESTION) },
                        onSeeAllActivity = { navController.navigate(NavigationKeys.TRANSACTIONS) },
                        onLogout = {
                            appViewModel.logOut()
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
                        incomeToExpenseRatio = appViewModel.incomeToExpenseRatio,
                        averageExpenseAmount = appViewModel.averageExpenseAmount,
                        transactionCount = appViewModel.transactions.size,
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
                },
                onLearnAboutSmartBills = { /* open info sheet once built */ }
            )
        }

        composable(NavigationKeys.GOAL_CREATE) {
            GoalCreateScreen(
                kind = pendingGoalKind,
                onSave = { name, targetAmount, description ->
                    appViewModel.addGoal(
                        name = name,
                        kind = pendingGoalKind,
                        targetAmount = targetAmount,
                        emoji = if (pendingGoalKind == GoalKind.SMART_BILL) "🧾" else "🎯",
                        description = description
                    )
                    navController.popBackStack(NavigationKeys.GOALS, inclusive = false)
                }
            )
        }

        composable(NavigationKeys.INGESTION) {
            val knownCategories = remember(appViewModel.transactions) {
                (DefaultCategoryRules.defaultCategories + appViewModel.transactions.map { it.category }).distinct()
            }
            IngestionScreen(
                knownCategories = knownCategories,
                onSave = { title, amount, category, type ->
                    appViewModel.addTransaction(title, amount, category, type)
                    navController.popBackStack()
                }
            )
        }

        composable(NavigationKeys.STATEMENT_SCAN) {
            val context = LocalContext.current
            val filePickerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    appViewModel.scanStatement(context, uri)
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
}

private fun NavHostController.navigateToMainGraph() {
    navigate(NavigationKeys.DASHBOARD) {
        popUpTo(graph.id) { inclusive = true }
    }
}

private fun NavHostController.navigateToWelcomeGraph() {
    navigate(NavigationKeys.WELCOME) {
        popUpTo(graph.id) { inclusive = true }
    }
}
