package com.hackkosice.server.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateWalletRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,
    @field:NotBlank
    @field:Size(min = 3, max = 3)
    val currency: String
)

data class AddWalletMemberRequest(
    @field:NotBlank
    val email: String
)

data class MockBankLinkRequest(
    @field:NotNull
    val walletId: Long,
    @field:NotBlank
    val bankName: String,
    @field:NotBlank
    val accountName: String,
    @field:NotBlank
    val iban: String,
    @field:NotNull
    @field:DecimalMin("0.00")
    val balance: BigDecimal,
    @field:NotBlank
    @field:Size(min = 3, max = 3)
    val currency: String
)

data class WalletResponse(
    val id: Long,
    val name: String,
    val currency: String,
    val totalBalance: BigDecimal,
    val members: List<WalletMemberResponse>,
    val bankConnections: List<BankConnectionResponse>
)

data class WalletMemberResponse(
    val id: Long,
    val userId: Long,
    val email: String,
    val fullName: String,
    val role: String
)

data class BankConnectionResponse(
    val id: Long,
    val walletId: Long,
    val userId: Long,
    val bankName: String,
    val accountName: String,
    val iban: String,
    val balance: BigDecimal,
    val currency: String,
    val status: String
)
