package com.example.myapplication.ui.home

data class HomeUiState(
    val title: String = "",
    val balanceFormatted: String = "0.00 €",
    val participantsSummary: String = "",
    val lastTransactionTitle: String = "",
    val lastTransactionAmount: String = ""
)
