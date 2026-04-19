package com.example.myapplication.di

import com.example.myapplication.data.repository.InMemoryWalletRepository
import com.example.myapplication.data.repository.WalletRepository

object AppGraph {
    val walletRepository: WalletRepository by lazy {
        InMemoryWalletRepository()
    }
}
