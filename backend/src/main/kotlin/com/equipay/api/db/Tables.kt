package com.equipay.api.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.postgresql.util.PGobject

class JsonbColumnType : ColumnType<String>() {
    override fun sqlType(): String = "JSONB"

    override fun valueFromDB(value: Any): String {
        return when (value) {
            is PGobject -> value.value ?: "{}"
            is String -> value
            else -> value.toString()
        }
    }

    override fun notNullValueToDB(value: String): Any {
        return PGobject().apply {
            type = "jsonb"
            this.value = value
        }
    }

    override fun nonNullValueToString(value: String): String {
        return "'${value.replace("'", "''")}'"
    }
}

fun Table.jsonbText(name: String): Column<String> = registerColumn(name, JsonbColumnType())

object Users : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val pinHash = varchar("pin_hash", 255).nullable()
    val displayName = varchar("display_name", 100).nullable()
    val emailVerified = bool("email_verified").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object RefreshTokens : UUIDTable("refresh_tokens") {
    val userId = reference("user_id", Users)
    val tokenHash = varchar("token_hash", 255).uniqueIndex()
    val deviceId = varchar("device_id", 128).nullable()
    val deviceName = varchar("device_name", 128).nullable()
    val expiresAt = timestamp("expires_at")
    val revokedAt = timestamp("revoked_at").nullable()
    val createdAt = timestamp("created_at")
}

object Accounts : UUIDTable("accounts") {
    val name = varchar("name", 100)
    val balanceCents = long("balance_cents").default(0)
    val currency = varchar("currency", 3).default("EUR")
    val ownerUserId = reference("owner_user_id", Users)
    val createdAt = timestamp("created_at")
}

object AccountMembers : Table("account_members") {
    val accountId = reference("account_id", Accounts)
    val userId = reference("user_id", Users)
    val role = varchar("role", 20).default("member")
    val contributedCents = long("contributed_cents").default(0)
    val joinedAt = timestamp("joined_at")
    override val primaryKey = PrimaryKey(accountId, userId)
}

object BankConnections : UUIDTable("bank_connections") {
    val userId = reference("user_id", Users)
    val bankCode = varchar("bank_code", 50)
    val externalAccountId = varchar("external_account_id", 128)
    val consentId = varchar("consent_id", 128)
    val consentToken = text("consent_token")
    val consentExpiresAt = timestamp("consent_expires_at")
    val active = bool("active").default(true)
    val createdAt = timestamp("created_at")
}

object VirtualCards : UUIDTable("virtual_cards") {
    val accountId = reference("account_id", Accounts)
    val userId = reference("user_id", Users)
    val last4 = varchar("last4", 4)
    val provider = varchar("provider", 50).default("internal_mock")
    val externalCardId = varchar("external_card_id", 128).nullable()
    val active = bool("active").default(true)
    val frozen = bool("frozen").default(false)
    val createdAt = timestamp("created_at")
}

object Transactions : UUIDTable("transactions") {
    val accountId = reference("account_id", Accounts)
    val initiatorUserId = reference("initiator_user_id", Users)
    val merchant = varchar("merchant", 255).nullable()
    val category = varchar("category", 50).nullable()
    val totalCents = long("total_cents")
    val currency = varchar("currency", 3).default("EUR")
    val splitMode = varchar("split_mode", 20)
    val status = varchar("status", 20).default("pending")
    val executedAt = timestamp("executed_at").nullable()
    val createdAt = timestamp("created_at")
}

object TransactionSplits : UUIDTable("transaction_splits") {
    val transactionId = reference("transaction_id", Transactions)
    val userId = reference("user_id", Users)
    val bankConnectionId = reference("bank_connection_id", BankConnections).nullable()
    val amountCents = long("amount_cents")
    val status = varchar("status", 20).default("pending")
    val externalPaymentId = varchar("external_payment_id", 128).nullable()
    val failureReason = text("failure_reason").nullable()
    val createdAt = timestamp("created_at")
}

object AuditLog : UUIDTable("audit_log") {
    val userId = reference("user_id", Users).nullable()
    val eventType = varchar("event_type", 50)
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val metadata = jsonbText("metadata").nullable()
    val createdAt = timestamp("created_at")
}