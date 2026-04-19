package com.example.myapplication.data.repository

import com.example.myapplication.data.model.SharedWallet
import kotlinx.coroutines.flow.StateFlow

interface WalletRepository {
    val wallet: StateFlow<SharedWallet>
    suspend fun addMoney(amountCents: Long)
    suspend fun splitExpenseEvenly(title: String, amountCents: Long)
    suspend fun toggleCardFreeze()
    suspend fun addParticipant(name: String, bankName: String, ibanMasked: String)
}
