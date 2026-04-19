package com.equipay.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.equipay.app.EquiPayApp
import com.equipay.app.auth.SessionState
import com.equipay.app.auth.SessionViewModel
import com.equipay.app.ui.components.BottomTab
import com.equipay.app.ui.components.EquiBottomBar
import com.equipay.app.ui.screens.ConnectBankScreen
import com.equipay.app.ui.screens.CreateGroupScreen
import com.equipay.app.ui.screens.CreateJoinScreen
import com.equipay.app.ui.screens.GroupDetailScreen
import com.equipay.app.ui.screens.GroupManageScreen
import com.equipay.app.ui.screens.HistoryScreen
import com.equipay.app.ui.screens.HomeScreen
import com.equipay.app.ui.screens.InsightsScreen
import com.equipay.app.ui.screens.InviteMemberScreen
import com.equipay.app.ui.screens.NewPaymentScreen
import com.equipay.app.ui.screens.VirtualCardScreen
import com.equipay.app.ui.screens.VoiceAssistantScreen
import com.equipay.app.ui.screens.auth.EmailVerificationScreen
import com.equipay.app.ui.screens.auth.LoginScreen
import com.equipay.app.ui.screens.auth.PinMode
import com.equipay.app.ui.screens.auth.PinScreen
import com.equipay.app.ui.screens.auth.SignUpScreen
import com.equipay.app.ui.screens.auth.SplashScreen
import com.equipay.app.ui.screens.auth.WelcomeScreen
import com.equipay.app.ui.viewmodels.AppState

@Composable
fun AppNavHost() {
    val app = EquiPayApp.instance
    val sessionVm: SessionViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SessionViewModel(app.tokenStore, app.authRepo) }
        }
    )
    val sessionState by sessionVm.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        sessionVm.bootstrap()
    }

    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionState.Unknown -> Unit

            is SessionState.LoggedOut -> {
                val route = navController.currentBackStackEntry?.destination?.route
                if (route == null || route == Screen.Splash.route || route in MAIN_ROUTES) {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            is SessionState.NeedsPin -> {
                val route = navController.currentBackStackEntry?.destination?.route
                if (route != Screen.PinSetup.route) {
                    navController.navigate(Screen.PinSetup.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            is SessionState.Authenticated -> {
                val route = navController.currentBackStackEntry?.destination?.route
                if (
                    route == null ||
                    route == Screen.Splash.route ||
                    route == Screen.LoginPin.route ||
                    route == Screen.LoginPassword.route ||
                    route == Screen.PinSetup.route ||
                    route == Screen.Welcome.route ||
                    route == Screen.SignUp.route ||
                    route?.startsWith("email_verify") == true
                ) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    AppScaffold(navController = navController, sessionVm = sessionVm, app = app)
}

private val MAIN_ROUTES = setOf(
    Screen.Home.route,
    Screen.Card.route,
    Screen.History.route,
    Screen.Insights.route
)

private const val INVITE_MEMBER_ROUTE = "invite_member/{accountId}"
private const val GROUP_MANAGE_ROUTE = "group_manage/{accountId}"
private const val GROUP_DETAIL_ROUTE = "group_detail/{accountId}"

@Composable
private fun AppScaffold(
    navController: NavHostController,
    sessionVm: SessionViewModel,
    app: EquiPayApp
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in MAIN_ROUTES

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val selected = when (currentRoute) {
                    Screen.Home.route -> BottomTab.Home
                    Screen.Card.route -> BottomTab.Card
                    Screen.History.route -> BottomTab.History
                    Screen.Insights.route -> BottomTab.Members
                    else -> BottomTab.Home
                }
                EquiBottomBar(
                    selected = selected,
                    onMicClick = { navController.navigate(Screen.VoiceAssistant.route) },
                    onSelect = { tab ->
                        val target = when (tab) {
                            BottomTab.Home -> Screen.Home.route
                            BottomTab.Members -> Screen.Insights.route
                            BottomTab.Card -> Screen.Card.route
                            BottomTab.History -> Screen.History.route
                        }
                        if (currentRoute != target) {
                            navController.navigate(target) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
        ) {
            composable(Screen.Splash.route) { SplashScreen() }

            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onSignUp = { navController.navigate(Screen.SignUp.route) },
                    onLogIn = { navController.navigate(Screen.LoginPassword.route) }
                )
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onBack = { navController.popBackStack() },
                    onRegistered = { email -> navController.navigate(Screen.EmailVerify.createRoute(email)) },
                    onGoToLogin = {
                        navController.navigate(Screen.LoginPassword.route) { popUpTo(Screen.Welcome.route) }
                    }
                )
            }

            composable(Screen.EmailVerify.route) { entry ->
                val email = entry.arguments?.getString("email") ?: ""
                EmailVerificationScreen(
                    email = email,
                    onBack = { navController.popBackStack() },
                    onVerified = { hasPin ->
                        val userId = app.tokenStore.getUserId().orEmpty()
                        val savedEmail = app.tokenStore.getEmail() ?: email
                        sessionVm.onAuthenticated(userId = userId, email = savedEmail, hasPin = hasPin)
                    }
                )
            }

            composable(Screen.LoginPassword.route) {
                LoginScreen(
                    onBack = { navController.popBackStack() },
                    onLoggedIn = { hasPin, email ->
                        val userId = app.tokenStore.getUserId().orEmpty()
                        sessionVm.onAuthenticated(userId = userId, email = email, hasPin = hasPin)
                    },
                    onGoToSignUp = {
                        navController.navigate(Screen.SignUp.route) { popUpTo(Screen.Welcome.route) }
                    }
                )
            }

            composable(Screen.LoginPin.route) {
                val email = app.tokenStore.getEmail() ?: ""
                PinScreen(
                    mode = PinMode.Login,
                    email = email,
                    onPinReady = {
                        val userId = app.tokenStore.getUserId().orEmpty()
                        val savedEmail = app.tokenStore.getEmail() ?: email
                        sessionVm.onAuthenticated(userId = userId, email = savedEmail, hasPin = true)
                    },
                    onFallbackToPassword = { navController.navigate(Screen.LoginPassword.route) }
                )
            }

            composable(Screen.PinSetup.route) {
                val email = app.tokenStore.getEmail() ?: ""
                PinScreen(mode = PinMode.SetUp, email = email, onPinReady = { sessionVm.onPinSet() })
            }

            // ===== HOME =====
            composable(Screen.Home.route) {
                HomeScreen(
                    onManageClick = {
                        val accountId = AppState.selectedAccountId.value
                        if (!accountId.isNullOrBlank()) {
                            navController.navigate(Screen.GroupManage.createRoute(accountId))
                        }
                    },
                    onSeeAllParticipants = {
                        val accountId = AppState.selectedAccountId.value
                        if (!accountId.isNullOrBlank()) {
                            navController.navigate(Screen.GroupDetail.createRoute(accountId))
                        }
                    },
                    onCreateGroup = { navController.navigate(Screen.CreateJoin.route) },
                    onCreateJoinClick = { navController.navigate(Screen.CreateJoin.route) },
                    onLogout = { sessionVm.logout() },
                    onInsightsClick = { navController.navigate(Screen.Insights.route) },
                    bottomPadding = innerPadding
                )
            }

            composable(Screen.CreateGroup.route) {
                CreateGroupScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { navController.popBackStack(Screen.Home.route, false) }
                )
            }

            composable(Screen.CreateJoin.route) {
                CreateJoinScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onJoined = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            // ===== GROUP MANAGEMENT =====
            composable(
                route = GROUP_MANAGE_ROUTE,
                arguments = listOf(navArgument("accountId") { type = NavType.StringType })
            ) { entry ->
                val accountId = entry.arguments?.getString("accountId").orEmpty()
                GroupManageScreen(
                    accountId = accountId,
                    onBack = { navController.popBackStack() },
                    onConnectBank = { navController.navigate(Screen.ConnectBank.route) }
                )
            }

            composable(
                route = GROUP_DETAIL_ROUTE,
                arguments = listOf(navArgument("accountId") { type = NavType.StringType })
            ) { entry ->
                val accountId = entry.arguments?.getString("accountId").orEmpty()
                GroupDetailScreen(
                    accountId = accountId,
                    onBack = { navController.popBackStack() },
                    onGroupLeft = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onInviteMember = { accId ->
                        navController.navigate("invite_member/$accId")
                    }
                )
            }

            // ===== INVITE MEMBER =====
            composable(
                route = INVITE_MEMBER_ROUTE,
                arguments = listOf(navArgument("accountId") { type = NavType.StringType })
            ) { entry ->
                val accountId = entry.arguments?.getString("accountId").orEmpty()
                InviteMemberScreen(
                    accountId = accountId,
                    onBack = { navController.popBackStack() },
                    onInvited = { navController.popBackStack() }
                )
            }

            // ===== VOICE ASSISTANT =====
            composable(Screen.VoiceAssistant.route) {
                VoiceAssistantScreen(
                    onBack = { navController.popBackStack() },
                    onConfirm = { parsed ->
                        navController.navigate(
                            Screen.NewPayment.createRoute(
                                amountCents = parsed.amountCents,
                                merchant = parsed.merchant,
                                split = parsed.splitMode,
                                category = parsed.category
                            )
                        ) { popUpTo(Screen.Home.route) }
                    }
                )
            }

            // ===== NEW PAYMENT =====
            composable(
                route = Screen.NewPayment.route,
                arguments = listOf(
                    navArgument("amount") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("merchant") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("split") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { entry ->
                val amount = entry.arguments?.getString("amount")?.toLongOrNull()
                val merchant = entry.arguments?.getString("merchant")?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                val split = entry.arguments?.getString("split")
                val category = entry.arguments?.getString("category")?.let { java.net.URLDecoder.decode(it, "UTF-8") }

                NewPaymentScreen(
                    amountCents = amount,
                    merchant = merchant,
                    splitMode = split,
                    category = category,
                    onBack = { navController.popBackStack() },
                    onClose = { navController.popBackStack(Screen.Home.route, false) },
                    onSuccess = { navController.popBackStack(Screen.Home.route, false) }
                )
            }

            composable(Screen.ConnectBank.route) {
                ConnectBankScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Card.route) {
                VirtualCardScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSplitMode = { },
                    onOpenConnectBank = { navController.navigate(Screen.ConnectBank.route) },
                    bottomPadding = innerPadding
                )
            }

            composable(Screen.Insights.route) {
                InsightsScreen(onBack = { navController.popBackStack() }, bottomPadding = innerPadding)
            }

            composable(Screen.History.route) {
                HistoryScreen(onBack = { navController.popBackStack() }, bottomPadding = innerPadding)
            }
        }
    }
}
