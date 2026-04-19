package com.hackkosice.server.repository

import com.hackkosice.server.entity.BankConnectionEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BankConnectionRepository : JpaRepository<BankConnectionEntity, Long> {
    fun findAllByUserId(userId: Long): List<BankConnectionEntity>
    fun findAllByWalletId(walletId: Long): List<BankConnectionEntity>
}
