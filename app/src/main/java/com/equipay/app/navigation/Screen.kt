package com.equipay.app.navigation

sealed class Screen(val route: String) {
    // Auth
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object SignUp : Screen("signup")
    data object EmailVerify : Screen("email_verify/{email}") {
        fun createRoute(email: String) = "email_verify/$email"
    }
    data object LoginPassword : Screen("login_password")
    data object LoginPin : Screen("login_pin")
    data object PinSetup : Screen("pin_setup")

    // Main
    data object Home : Screen("home")
    data object VoiceAssistant : Screen("voice")
    data object NewPayment : Screen("new_payment?amount={amount}&merchant={merchant}&split={split}&category={category}") {
        const val baseRoute = "new_payment"
        fun createRoute(amountCents: Long? = null, merchant: String? = null, split: String? = null, category: String? = null): String {
            val params = buildList {
                amountCents?.let { add("amount=$it") }
                merchant?.let { add("merchant=${java.net.URLEncoder.encode(it, "UTF-8")}") }
                split?.let { add("split=$it") }
                category?.let { add("category=${java.net.URLEncoder.encode(it, "UTF-8")}") }
            }
            return if (params.isEmpty()) baseRoute else "$baseRoute?${params.joinToString("&")}"
        }
    }
    data object CreateGroup : Screen("create_group")
    data object ConnectBank : Screen("connect_bank")
    data object Card : Screen("card")
    data object Insights : Screen("insights")
    data object History : Screen("history")

    // Group management
    data object CreateJoin : Screen("create_join")

    // Group management
    data object GroupManage : Screen("group_manage/{accountId}") {
        fun createRoute(accountId: String) = "group_manage/$accountId"
    }
    data object GroupDetail : Screen("group_detail/{accountId}") {
        fun createRoute(accountId: String) = "group_detail/$accountId"
    }
    data object Profile : Screen("profile")
}
