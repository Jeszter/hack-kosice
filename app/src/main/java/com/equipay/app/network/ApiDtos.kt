package com.equipay.app.network

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val displayName: String?
)

@Serializable
data class UpdateProfileRequest(val displayName: String?)

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val balanceCents: Long,
    val currency: String,
    val ownerUserId: String,
    val members: List<MemberDto>,
    val createdAt: String
)

@Serializable
data class MemberDto(
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val role: String,
    val contributedCents: Long
)

@Serializable
data class CreateAccountRequest(val name: String, val memberEmails: List<String> = emptyList())

@Serializable
data class InviteMemberRequest(val email: String)

@Serializable
data class RequestInviteCodeRequest(val email: String)

@Serializable
data class RequestInviteCodeResponse(
    val ok: Boolean = true,
    val message: String
)

@Serializable
data class ConfirmInviteCodeRequest(
    val email: String,
    val code: String
)

@Serializable
data class ConfirmInviteCodeResponse(
    val ok: Boolean = true,
    val member: MemberDto
)

@Serializable
data class AddFundsRequest(val amountCents: Long)

@Serializable
data class AvailableBankDto(
    val code: String,
    val name: String,
    val psd2Supported: Boolean
)

@Serializable
data class BankConnectionDto(
    val id: String,
    val bankCode: String,
    val externalAccountId: String,
    val active: Boolean,
    val consentExpiresAt: String,
    val createdAt: String
)

@Serializable
data class ConnectBankRequest(
    val bankCode: String,
    val externalAccountId: String = "",
    val consentToken: String = ""
)

@Serializable
data class LinkedBalanceSourceDto(
    val userId: String,
    val connectionId: String,
    val bankCode: String,
    val externalAccountId: String,
    val balanceCents: Long,
    val currency: String,
    val iban: String? = null,
    val accountName: String? = null
)

@Serializable
data class LinkedBalanceSummaryDto(
    val accountId: String,
    val currency: String,
    val totalBalanceCents: Long,
    val sources: List<LinkedBalanceSourceDto>
)

@Serializable
data class VirtualCardDto(
    val id: String,
    val accountId: String,
    val userId: String,
    val last4: String,
    val provider: String,
    val active: Boolean,
    val frozen: Boolean,
    val createdAt: String
)

@Serializable
data class CreateCardRequest(val accountId: String)

@Serializable
data class FreezeCardRequest(val frozen: Boolean)

@Serializable
data class TransactionSplitDto(
    val id: String,
    val userId: String,
    val userEmail: String,
    val userName: String? = null,
    val amountCents: Long,
    val status: String,
    val externalPaymentId: String? = null,
    val failureReason: String? = null
)

@Serializable
data class TransactionDto(
    val id: String,
    val accountId: String,
    val initiatorUserId: String,
    val initiatorName: String? = null,
    val merchant: String? = null,
    val category: String? = null,
    val totalCents: Long,
    val currency: String,
    val splitMode: String,
    val status: String,
    val executedAt: String? = null,
    val createdAt: String,
    val splits: List<TransactionSplitDto>
)

@Serializable
data class CreateTransactionRequest(
    val accountId: String,
    val merchant: String,
    val category: String? = null,
    val totalCents: Long,
    val splitMode: String = "equal",
    val customSplit: Map<String, Long>? = null
)

@Serializable
data class InsightsDto(
    val spendingThisWeekCents: Long,
    val deltaPercent: Double,
    val weeklySpend: List<DailySpendDto>,
    val topCategories: List<CategoryDto>
)

@Serializable
data class DailySpendDto(val dayShort: String, val amountCents: Long)

@Serializable
data class CategoryDto(val category: String, val amountCents: Long)

@Serializable
data class VoiceParseRequest(val transcript: String, val accountId: String? = null)

@Serializable
data class ReceiptParseRequest(
    val imageBase64: String,
    val mimeType: String = "image/jpeg",
    val accountId: String? = null
)

@Serializable
data class VoiceParseResponse(
    val merchant: String,
    val amountCents: Long,
    val splitMode: String,
    val category: String? = null,
    val confidence: Double = 0.8,
    val confirmationText: String
)

@Serializable
data class SmartSplitRequest(val accountId: String, val totalCents: Long, val merchant: String? = null)

@Serializable
data class SmartSplitItem(val userId: String, val amountCents: Long)

@Serializable
data class SmartSplitResponse(
    val mode: String,
    val split: List<SmartSplitItem>,
    val suggestion: String
)

@Serializable
data class RebalanceSuggestionDto(val message: String)

@Serializable
data class MonthlyLimitDto(
    val accountId: String,
    val limitCents: Long,
    val spentThisMonthCents: Long,
    val remainingCents: Long,
    val currency: String
)

@Serializable
data class SetMonthlyLimitRequest(val limitCents: Long)

@Serializable
data class LeaveGroupResponse(val ok: Boolean = true, val message: String = "Left group")

@Serializable
data class KickMemberRequest(val userId: String)

@Serializable
data class AiChatRequest(
    val message: String,
    val accountId: String? = null,
    val context: String? = null
)

@Serializable
data class AiChatResponse(
    val reply: String,
    val actionType: String? = null // "create_transaction", "show_balance", "show_history", etc.
)