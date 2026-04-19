package com.equipay.api.banks

import com.equipay.api.db.AccountMembers
import com.equipay.api.db.BankConnections
import com.equipay.api.db.dbQuery
import com.equipay.api.redis.RedisClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

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
data class AvailableBankDto(
    val code: String,
    val name: String,
    val psd2Supported: Boolean
)

@Serializable
data class StartTatraConnectResponse(
    val authorizeUrl: String
)

@Serializable
data class PendingTatraAuth(
    val userId: String,
    val state: String,
    val consentId: String,
    val codeVerifier: String,
    val createdAt: String
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

class BankService(
    private val tatraClient: TatraBankClient,
    private val redis: RedisClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun pendingKey(state: String) = "tatra:pending:$state"

    fun availableBanks(): List<AvailableBankDto> = listOf(
        AvailableBankDto("TATRA", "Tatra banka", true),
        AvailableBankDto("SLSP", "SLSP", false),
        AvailableBankDto("CSOB", "ČSOB", false),
        AvailableBankDto("VUB", "VÚB", false)
    )

    fun listForUser(userId: UUID): List<BankConnectionDto> = dbQuery {
        BankConnections.select { BankConnections.userId eq userId }
            .orderBy(BankConnections.createdAt to SortOrder.DESC)
            .map { it.toDto() }
    }

    suspend fun startTatraConnect(userId: UUID): StartTatraConnectResponse {
        val clientToken = tatraClient.getClientCredentialsToken()
        val consent = tatraClient.createConsent(clientToken.accessToken)
        val pkce = tatraClient.createPkcePair()
        val state = UUID.randomUUID().toString()

        val pendingAuth = PendingTatraAuth(
            userId = userId.toString(),
            state = state,
            consentId = consent.consentId,
            codeVerifier = pkce.verifier,
            createdAt = Instant.now().toString()
        )

        redis.setEx(
            pendingKey(state),
            15 * 60,
            json.encodeToString(PendingTatraAuth.serializer(), pendingAuth)
        )

        val authorizeUrl = tatraClient.buildAuthorizeUrl(
            consentId = consent.consentId,
            state = state,
            codeChallenge = pkce.challenge
        )

        println("Saved pending Tatra auth to Redis for state=$state userId=$userId")
        println("Tatra authorize URL = $authorizeUrl")

        return StartTatraConnectResponse(authorizeUrl)
    }

    suspend fun finishTatraConnect(state: String, code: String): BankConnectionDto {
        println("finishTatraConnect: state=$state")
        println("finishTatraConnect: code=$code")

        val pendingRaw = redis.get(pendingKey(state)) ?: error("Invalid or expired state")
        redis.del(pendingKey(state))

        val pendingAuth = json.decodeFromString(PendingTatraAuth.serializer(), pendingRaw)
        println("finishTatraConnect: pending auth found for userId=${pendingAuth.userId}")

        val token = tatraClient.exchangeCodeForToken(code, pendingAuth.codeVerifier)
        println("finishTatraConnect: token exchanged successfully")

        val accounts = tatraClient.readAccounts(token.accessToken, pendingAuth.consentId)
        println("finishTatraConnect: accounts fetched, count=${accounts.accounts.size}")

        val externalAccountId = accounts.accounts.firstOrNull()?.resourceId
            ?: error("No accounts returned by Tatra")

        val id = dbQuery {
            BankConnections.insertAndGetId {
                it[userId] = UUID.fromString(pendingAuth.userId)
                it[bankCode] = "TATRA"
                it[BankConnections.externalAccountId] = externalAccountId
                it[consentId] = pendingAuth.consentId
                it[consentToken] = token.accessToken
                it[consentExpiresAt] = Instant.now().plusSeconds(90L * 86400)
                it[active] = true
                it[createdAt] = Instant.now()
            }.value
        }

        println("finishTatraConnect: connection saved with id=$id")

        return dbQuery {
            BankConnections.select { BankConnections.id eq id }.single().toDto()
        }
    }

    suspend fun getLinkedBalanceForAccount(accountId: UUID, requesterId: UUID): LinkedBalanceSummaryDto {
        val memberUserIds = dbQuery {
            val isMember = AccountMembers.select {
                (AccountMembers.accountId eq accountId) and (AccountMembers.userId eq requesterId)
            }.any()

            if (!isMember) {
                error("Not a member")
            }

            AccountMembers.select { AccountMembers.accountId eq accountId }
                .map { it[AccountMembers.userId].value }
        }

        if (memberUserIds.isEmpty()) {
            return LinkedBalanceSummaryDto(
                accountId = accountId.toString(),
                currency = "EUR",
                totalBalanceCents = 0,
                sources = emptyList()
            )
        }

        val connections = dbQuery {
            val result = mutableListOf<StoredBankConnection>()

            for (memberUserId in memberUserIds) {
                val rows = BankConnections.select {
                    (BankConnections.userId eq memberUserId) and (BankConnections.active eq true)
                }
                    .orderBy(BankConnections.createdAt to SortOrder.DESC)
                    .map { row ->
                        StoredBankConnection(
                            id = row[BankConnections.id].value,
                            userId = row[BankConnections.userId].value,
                            bankCode = row[BankConnections.bankCode],
                            externalAccountId = row[BankConnections.externalAccountId],
                            consentId = row[BankConnections.consentId],
                            consentToken = row[BankConnections.consentToken]
                        )
                    }

                result += rows
            }

            result
        }

        val sources = mutableListOf<LinkedBalanceSourceDto>()

        for (connection in connections) {
            when (connection.bankCode.uppercase()) {
                "TATRA" -> {
                    try {
                        val accounts = tatraClient.readAccounts(
                            userAccessToken = connection.consentToken,
                            consentId = connection.consentId
                        )

                        val matchedAccount = accounts.accounts.firstOrNull {
                            it.resourceId == connection.externalAccountId
                        }

                        if (matchedAccount == null) {
                            println("Linked account not found for connection=${connection.id}, externalAccountId=${connection.externalAccountId}")
                            continue
                        }

                        val amount = matchedAccount.balances
                            .mapNotNull { it.balanceAmount }
                            .firstOrNull { !it.amount.isNullOrBlank() }

                        val balanceCents = if (amount != null) {
                            amountToCents(amount.amount ?: "0")
                        } else {
                            println("No balance returned for connection=${connection.id}, using fallback 0")
                            0L
                        }

                        val currency = amount?.currency ?: matchedAccount.currency ?: "EUR"

                        if (currency != "EUR") {
                            println("Skipping non-EUR linked account ${connection.id}: currency=$currency")
                            continue
                        }

                        sources += LinkedBalanceSourceDto(
                            userId = connection.userId.toString(),
                            connectionId = connection.id.toString(),
                            bankCode = connection.bankCode,
                            externalAccountId = connection.externalAccountId,
                            balanceCents = balanceCents,
                            currency = currency,
                            iban = matchedAccount.iban,
                            accountName = matchedAccount.name
                        )
                    } catch (e: Exception) {
                        println("CRITICAL: Failed to fetch linked balance for connection=${connection.id}: ${e.message}")
                    }
                }
            }
        }

        val totalBalanceCents = sources.sumOf { it.balanceCents }

        return LinkedBalanceSummaryDto(
            accountId = accountId.toString(),
            currency = "EUR",
            totalBalanceCents = totalBalanceCents,
            sources = sources
        )
    }

    fun disconnect(userId: UUID, connectionId: UUID) {
        dbQuery {
            BankConnections.update({
                (BankConnections.id eq connectionId) and (BankConnections.userId eq userId)
            }) {
                it[active] = false
            }
        }
    }

    private fun amountToCents(amount: String): Long {
        return BigDecimal(amount)
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    private fun ResultRow.toDto() = BankConnectionDto(
        id = this[BankConnections.id].value.toString(),
        bankCode = this[BankConnections.bankCode],
        externalAccountId = this[BankConnections.externalAccountId],
        active = this[BankConnections.active],
        consentExpiresAt = this[BankConnections.consentExpiresAt].toString(),
        createdAt = this[BankConnections.createdAt].toString()
    )

    private data class StoredBankConnection(
        val id: UUID,
        val userId: UUID,
        val bankCode: String,
        val externalAccountId: String,
        val consentId: String,
        val consentToken: String
    )
}