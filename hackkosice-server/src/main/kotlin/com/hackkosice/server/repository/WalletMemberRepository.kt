package com.hackkosice.server.repository

import com.hackkosice.server.entity.WalletEntity
import com.hackkosice.server.entity.WalletMemberEntity
import org.springframework.data.jpa.repository.JpaRepository

interface WalletMemberRepository : JpaRepository<WalletMemberEntity, Long> {
    fun findAllByUserId(userId: Long): List<WalletMemberEntity>
    fun existsByWalletIdAndUserId(walletId: Long, userId: Long): Boolean
    fun findByWalletIdAndUserId(walletId: Long, userId: Long): WalletMemberEntity?
    fun findAllByWallet(wallet: WalletEntity): List<WalletMemberEntity>
}
