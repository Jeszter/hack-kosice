package com.example.myapplication.data.model

data class TransactionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amountCents: Long,
    val type: TransactionType,
    val createdAt: String
)

enum class TransactionType {
    DEBIT,
    CREDIT
}
