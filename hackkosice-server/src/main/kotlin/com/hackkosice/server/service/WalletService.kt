package com.hackkosice.server.service

import com.hackkosice.server.dto.AddWalletMemberRequest
import com.hackkosice.server.dto.BankConnectionResponse
import com.hackkosice.server.dto.CreateWalletRequest
import com.hackkosice.server.dto.MockBankLinkRequest
import com.hackkosice.server.dto.WalletMemberResponse
import com.hackkosice.server.dto.WalletResponse
import com.hackkosice.server.entity.BankConnectionEntity
import com.hackkosice.server.entity.UserEntity
import com.hackkosice.server.entity.WalletEntity
import com.hackkosice.server.entity.WalletMemberEntity
import com.hackkosice.server.entity.WalletMemberRole
import com.hackkosice.server.repository.BankConnectionRepository
import com.hackkosice.server.repository.UserRepository
import com.hackkosice.server.repository.WalletMemberRepository
import com.hackkosice.server.repository.WalletRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val walletMemberRepository: WalletMemberRepository,
    private val userRepository: UserRepository,
    private val bankConnectionRepository: BankConnectionRepository
) {
    @Transactional
    fun createWallet(currentUser: UserEntity, request: CreateWalletRequest): WalletResponse {
        val wallet = walletRepository.save(
            WalletEntity(
                name = request.name.trim(),
                currency = request.currency.trim().uppercase(),
                totalBalance = BigDecimal.ZERO
            )
        )

        walletMemberRepository.save(
            WalletMemberEntity(
                wallet = wallet,
                user = currentUser,
                role = WalletMemberRole.OWNER
            )
        )

        return getWallet(wallet.id!!, currentUser)
    }

    fun getMyWallets(currentUser: UserEntity): List<WalletResponse> {
        return walletMemberRepository.findAllByUserId(currentUser.id!!)
            .map { getWallet(it.wallet.id!!, currentUser) }
    }

    fun getWallet(walletId: Long, currentUser: UserEntity): WalletResponse {
        ensureMember(walletId, currentUser.id!!)
        val wallet = walletRepository.findById(walletId)
            .orElseThrow { IllegalArgumentException("Wallet not found") }
        return wallet.toResponse()
    }

    @Transactional
    fun addMember(walletId: Long, request: AddWalletMemberRequest, currentUser: UserEntity): WalletResponse {
        val membership = ensureMember(walletId, currentUser.id!!)
        if (membership.role != WalletMemberRole.OWNER) {
            error("Only wallet owner can add members")
        }

        val invitedUser = userRepository.findByEmail(request.email.trim().lowercase())
            .orElseThrow { IllegalArgumentException("User not found") }

        if (!walletMemberRepository.existsByWalletIdAndUserId(walletId, invitedUser.id!!)) {
            walletMemberRepository.save(
                WalletMemberEntity(
                    wallet = membership.wallet,
                    user = invitedUser,
                    role = WalletMemberRole.MEMBER
                )
            )
        }

        return getWallet(walletId, currentUser)
    }

    @Transactional
    fun mockLinkBankAccount(request: MockBankLinkRequest, currentUser: UserEntity): BankConnectionResponse {
        ensureMember(request.walletId, currentUser.id!!)
        val wallet = walletRepository.findById(request.walletId)
            .orElseThrow { IllegalArgumentException("Wallet not found") }

        val connection = bankConnectionRepository.save(
            BankConnectionEntity(
                wallet = wallet,
                user = currentUser,
                bankName = request.bankName.trim(),
                accountName = request.accountName.trim(),
                iban = request.iban.trim(),
                balance = request.balance,
                currency = request.currency.trim().uppercase()
            )
        )

        wallet.totalBalance = bankConnectionRepository.findAllByWalletId(wallet.id!!)
            .fold(BigDecimal.ZERO) { acc, item -> acc + item.balance }
        walletRepository.save(wallet)

        return connection.toResponse()
    }

    fun getMyBankConnections(currentUser: UserEntity): List<BankConnectionResponse> {
        return bankConnectionRepository.findAllByUserId(currentUser.id!!).map { it.toResponse() }
    }

    private fun ensureMember(walletId: Long, userId: Long): WalletMemberEntity {
        return walletMemberRepository.findByWalletIdAndUserId(walletId, userId)
            ?: throw IllegalArgumentException("Access denied to this wallet")
    }

    private fun WalletEntity.toResponse(): WalletResponse {
        return WalletResponse(
            id = id!!,
            name = name,
            currency = currency,
            totalBalance = totalBalance,
            members = members.sortedBy { it.id }.map {
                WalletMemberResponse(
                    id = it.id!!,
                    userId = it.user.id!!,
                    email = it.user.email,
                    fullName = it.user.fullName,
                    role = it.role.name
                )
            },
            bankConnections = bankConnections.sortedBy { it.id }.map { it.toResponse() }
        )
    }

    private fun BankConnectionEntity.toResponse(): BankConnectionResponse {
        return BankConnectionResponse(
            id = id!!,
            walletId = wallet.id!!,
            userId = user.id!!,
            bankName = bankName,
            accountName = accountName,
            iban = iban,
            balance = balance,
            currency = currency,
            status = status
        )
    }
}
