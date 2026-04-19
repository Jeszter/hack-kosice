package com.equipay.app.data

data class Participant(
    val id: String,
    val name: String,
    val balance: Double,
    val avatarSeed: Int = name.hashCode()
)

data class Transaction(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val category: Category,
    val date: String,
    val isToday: Boolean
)

enum class Category { FOOD, TRANSPORT, SHOPPING, ENTERTAINMENT, OTHER }

data class Bank(
    val id: String,
    val code: String,
    val name: String,
    val connected: Boolean
)

object MockData {
    val participants = listOf(
        Participant("1", "You", 300.0),
        Participant("2", "Alex", 400.0),
        Participant("3", "Maria", 540.0),
        Participant("4", "David", 0.0),
    )

    val totalBalance = 1240.00
    val weeklyDelta = 96.50

    val transactionsToday = listOf(
        Transaction("t1", "Pizza Place", "Split 4 ways • 10,00 € each", -40.0, Category.FOOD, "Today", true),
        Transaction("t2", "Uber", "You paid", -12.40, Category.TRANSPORT, "Today", true),
    )

    val transactionsYesterday = listOf(
        Transaction("y1", "Museum Tickets", "Split 3 ways • 20,00 € each", -60.0, Category.ENTERTAINMENT, "Yesterday", false),
        Transaction("y2", "Grocery Store", "Maria paid", -34.90, Category.SHOPPING, "Yesterday", false),
    )

    val banks = listOf(
        Bank("tb", "TB", "Tatra banka", connected = false),
        Bank("slsp", "SLSP", "SLSP", connected = false),
        Bank("csob", "ČSOB", "ČSOB", connected = false),
        Bank("vub", "m", "VÚB", connected = false),
    )

    val weeklySpend = listOf(
        "Mon" to 220f,
        "Tue" to 310f,
        "Wed" to 240f,
        "Thu" to 260f,
        "Fri" to 130f,
        "Sun" to 230f,
    )

    val spendingThisWeek = 320.50
    val deltaLastWeek = 12.5
}
