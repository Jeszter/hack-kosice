package com.example.myapplication.data.model

data class SharedWallet(
    val walletId: String,
    val walletName: String,
    val totalBalanceCents: Long,
    val participants: List<Participant>,
    val recentTransactions: List<TransactionItem>,
    val virtualCard: VirtualCard,
    val messages: List<AssistantMessage>
)
