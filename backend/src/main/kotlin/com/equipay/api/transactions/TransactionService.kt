package com.equipay.api.transactions

import com.equipay.api.accounts.AccountService
import com.equipay.api.banks.TatraBankClient
import com.equipay.api.db.AccountMembers
import com.equipay.api.db.Accounts
import com.equipay.api.db.BankConnections
import com.equipay.api.db.TransactionSplits
import com.equipay.api.db.Transactions
import com.equipay.api.db.Users
import com.equipay.api.db.dbQuery
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import java.time.Instant
import java.util.UUID

// ========= DTO =========

@Serializable data class CreateTransactionRequest(
    val accountId: String,
    val merchant: String,
    val category: String? = null,
    val totalCents: Long,
    val splitMode: String = "equal",  // "equal" / "smart" / "solo"
    /** Optional: для "smart" можно передать ручные доли userId -> cents (сумма должна равняться total) */
    val customSplit: Map<String, Long>? = null
)

@Serializable data class TransactionSplitDto(
    val id: String,
    val userId: String,
    val userEmail: String,
    val userName: String?,
    val amountCents: Long,
    val status: String,
    val externalPaymentId: String? = null,
    val failureReason: String? = null
)

@Serializable data class TransactionDto(
    val id: String,
    val accountId: String,
    val initiatorUserId: String,
    val initiatorName: String?,
    val merchant: String?,
    val category: String?,
    val totalCents: Long,
    val currency: String,
    val splitMode: String,
    val status: String,
    val executedAt: String?,
    val createdAt: String,
    val splits: List<TransactionSplitDto>
)

// ========= Service =========

class TransactionService(
    private val accountService: AccountService,
    private val tatra: TatraBankClient
) {

    fun create(initiatorId: UUID, req: CreateTransactionRequest): TransactionDto {
        val accountId = UUID.fromString(req.accountId)
        if (!accountService.isMember(accountId, initiatorId)) error("Not a member of this account")
        if (req.totalCents <= 0) error("Amount must be positive")

        // 1. Найдём всех members
        val memberIds: List<UUID> = dbQuery {
            AccountMembers.select { AccountMembers.accountId eq accountId }
                .map { it[AccountMembers.userId].value }
        }
        if (memberIds.isEmpty()) error("No members")

        // 2. Посчитаем split по режиму
        val splits: Map<UUID, Long> = when (req.splitMode) {
            "solo" -> mapOf(initiatorId to req.totalCents)
            "smart", "equal" -> {
                if (req.customSplit != null) {
                    val parsed = req.customSplit.mapKeys { UUID.fromString(it.key) }
                    val sum = parsed.values.sum()
                    if (sum != req.totalCents) error("Custom split sum ($sum) != total (${req.totalCents})")
                    parsed
                } else {
                    splitEqually(memberIds, req.totalCents)
                }
            }
            else -> error("Unknown split mode: ${req.splitMode}")
        }

        // 3. Создаём транзакцию в БД
        val now = Instant.now()
        val txId = dbQuery {
            val id = Transactions.insertAndGetId {
                it[Transactions.accountId] = accountId
                it[initiatorUserId] = initiatorId
                it[merchant] = req.merchant
                it[category] = req.category
                it[totalCents] = req.totalCents
                it[splitMode] = req.splitMode
                it[status] = "fanout_in_progress"
                it[createdAt] = now
            }.value

            splits.forEach { (uid, amount) ->
                TransactionSplits.insert {
                    it[transactionId] = id
                    it[userId] = uid
                    it[amountCents] = amount
                    it[status] = "pending"
                    it[createdAt] = now
                }
            }
            id
        }

        // 4. Fan-out: для каждого участника пытаемся выполнить платёж с его bank connection
        val accountName = dbQuery { Accounts.select { Accounts.id eq accountId }.single()[Accounts.name] }
        splits.forEach { (uid, amount) ->
            executeFanoutPayment(txId, uid, amount, req.merchant, accountName)
        }

        // 5. Определяем итоговый статус
        val finalStatus = dbQuery {
            val splitStatuses = TransactionSplits
                .select { TransactionSplits.transactionId eq txId }
                .map { it[TransactionSplits.status] }

            val status = when {
                splitStatuses.all { it == "completed" } -> "completed"
                splitStatuses.any { it == "completed" } -> "partial"
                else -> "failed"
            }

            Transactions.update({ Transactions.id eq txId }) {
                it[Transactions.status] = status
                it[executedAt] = Instant.now()
            }

            // Обновить balance группового счёта только при успехе
            if (status == "completed") {
                Accounts.update({ Accounts.id eq accountId }) {
                    with(SqlExpressionBuilder) {
                        it.update(balanceCents, balanceCents - req.totalCents)
                    }
                }
            }
            status
        }

        return getById(txId)!!
    }

    /**
     * Пытаемся списать `amountCents` с первого активного bank connection юзера.
     * Если нет — помечаем split как failed.
     */
    private fun executeFanoutPayment(
        txId: UUID,
        userId: UUID,
        amountCents: Long,
        merchant: String,
        accountName: String
    ) {
        val connection = dbQuery {
            BankConnections.select {
                (BankConnections.userId eq userId) and (BankConnections.active eq true)
            }.firstOrNull()
        }

        if (connection == null) {
            dbQuery {
                TransactionSplits.update({
                    (TransactionSplits.transactionId eq txId) and (TransactionSplits.userId eq userId)
                }) {
                    it[status] = "failed"
                    it[failureReason] = "No active bank connection"
                }
            }
            return
        }

        val bankCode = connection[BankConnections.bankCode]
        val externalAccountId = connection[BankConnections.externalAccountId]
        val consent = connection[BankConnections.consentToken]

        val result = if (bankCode == "TATRA") {
            // Реальный (или полу-реальный) вызов Tatra
            try {
                val resp = tatra.initiatePayment(
                    debtorIban = externalAccountId,
                    creditorIban = "SK0011000000009999999999",  // mock группового креденциала
                    amountCents = amountCents,
                    remittance = "EquiPay: $accountName / $merchant",
                    consentToken = consent
                )
                resp.paymentId to (resp.status in setOf("ACSC", "ACCP", "ACSP"))
            } catch (e: Exception) {
                null to false
            }
        } else {
            // Моки других банков — всегда success в хакатоне
            "mock_${UUID.randomUUID().toString().take(12)}" to true
        }

        dbQuery {
            TransactionSplits.update({
                (TransactionSplits.transactionId eq txId) and (TransactionSplits.userId eq userId)
            }) {
                it[bankConnectionId] = connection[BankConnections.id]
                it[externalPaymentId] = result.first
                it[status] = if (result.second) "completed" else "failed"
                if (!result.second) it[failureReason] = "Bank declined"
            }
            if (result.second) {
                // Увеличиваем contributed для этого юзера
                val accountId = Transactions.select { Transactions.id eq txId }
                    .single()[Transactions.accountId].value
                AccountMembers.update({
                    (AccountMembers.accountId eq accountId) and (AccountMembers.userId eq userId)
                }) {
                    with(SqlExpressionBuilder) {
                        it.update(contributedCents, contributedCents + amountCents)
                    }
                }
            }
        }
    }

    private fun splitEqually(memberIds: List<UUID>, totalCents: Long): Map<UUID, Long> {
        val n = memberIds.size
        val base = totalCents / n
        val remainder = totalCents % n
        // Раскидываем остаток по центу первым N юзерам — чтобы сумма точно совпала
        return memberIds.mapIndexed { i, uid ->
            uid to (base + if (i < remainder) 1 else 0)
        }.toMap()
    }

    fun getById(txId: UUID): TransactionDto? = dbQuery {
        val row = Transactions.select { Transactions.id eq txId }.singleOrNull() ?: return@dbQuery null
        hydrate(row)
    }

    fun listForAccount(accountId: UUID, limit: Int = 50): List<TransactionDto> = dbQuery {
        Transactions.select { Transactions.accountId eq accountId }
            .orderBy(Transactions.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { hydrate(it) }
    }

    fun listForUser(userId: UUID, limit: Int = 100): List<TransactionDto> = dbQuery {
        val accountIds = AccountMembers.select { AccountMembers.userId eq userId }
            .map { it[AccountMembers.accountId].value }
        if (accountIds.isEmpty()) return@dbQuery emptyList()
        Transactions.select { Transactions.accountId inList accountIds }
            .orderBy(Transactions.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { hydrate(it) }
    }

    private fun hydrate(row: ResultRow): TransactionDto {
        val txId = row[Transactions.id].value
        val initiatorRow = Users.select { Users.id eq row[Transactions.initiatorUserId] }.single()
        val splits = (TransactionSplits innerJoin Users)
            .select { TransactionSplits.transactionId eq txId }
            .map {
                TransactionSplitDto(
                    id = it[TransactionSplits.id].value.toString(),
                    userId = it[TransactionSplits.userId].value.toString(),
                    userEmail = it[Users.email],
                    userName = it[Users.displayName],
                    amountCents = it[TransactionSplits.amountCents],
                    status = it[TransactionSplits.status],
                    externalPaymentId = it[TransactionSplits.externalPaymentId],
                    failureReason = it[TransactionSplits.failureReason]
                )
            }
        return TransactionDto(
            id = txId.toString(),
            accountId = row[Transactions.accountId].value.toString(),
            initiatorUserId = row[Transactions.initiatorUserId].value.toString(),
            initiatorName = initiatorRow[Users.displayName] ?: initiatorRow[Users.email],
            merchant = row[Transactions.merchant],
            category = row[Transactions.category],
            totalCents = row[Transactions.totalCents],
            currency = row[Transactions.currency],
            splitMode = row[Transactions.splitMode],
            status = row[Transactions.status],
            executedAt = row[Transactions.executedAt]?.toString(),
            createdAt = row[Transactions.createdAt].toString(),
            splits = splits
        )
    }
}
