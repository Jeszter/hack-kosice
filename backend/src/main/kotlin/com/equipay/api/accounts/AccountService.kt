package com.equipay.api.accounts

import com.equipay.api.db.AccountMembers
import com.equipay.api.db.Accounts
import com.equipay.api.db.Users
import com.equipay.api.db.dbQuery
import com.equipay.api.email.EmailService
import com.equipay.api.redis.RedisClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

@Serializable
data class CreateAccountRequest(val name: String, val memberEmails: List<String> = emptyList())

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
    val displayName: String?,
    val role: String,
    val contributedCents: Long
)

@Serializable
data class InviteMemberRequest(val email: String)

@Serializable
data class AddFundsRequest(val amountCents: Long)

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
private data class PendingFamilyInvite(
    val accountId: String,
    val requesterId: String,
    val email: String,
    val code: String,
    val createdAt: String
)

class AccountService(
    private val emailService: EmailService,
    private val redis: RedisClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun create(ownerId: UUID, req: CreateAccountRequest): AccountDto {
        val now = Instant.now()
        val accountId = dbQuery {
            val id = Accounts.insertAndGetId {
                it[name] = req.name
                it[ownerUserId] = ownerId
                it[balanceCents] = 0
                it[createdAt] = now
            }.value

            AccountMembers.insert {
                it[this.accountId] = id
                it[userId] = ownerId
                it[role] = "owner"
                it[joinedAt] = now
            }

            req.memberEmails.distinct()
                .filter { it.isNotBlank() }
                .map { it.trim().lowercase() }
                .forEach { memberEmail ->
                    val memberRow = Users.select { Users.email eq memberEmail }.singleOrNull()
                    if (memberRow != null && memberRow[Users.id].value != ownerId) {
                        AccountMembers.insert {
                            it[this.accountId] = id
                            it[userId] = memberRow[Users.id]
                            it[role] = "member"
                            it[joinedAt] = now
                        }
                    }
                }
            id
        }
        return getById(accountId) ?: error("just created")
    }

    fun getById(accountId: UUID): AccountDto? = dbQuery {
        val row = Accounts.select { Accounts.id eq accountId }.singleOrNull() ?: return@dbQuery null
        val members = (AccountMembers innerJoin Users)
            .select { AccountMembers.accountId eq accountId }
            .map {
                MemberDto(
                    userId = it[AccountMembers.userId].value.toString(),
                    email = it[Users.email],
                    displayName = it[Users.displayName],
                    role = it[AccountMembers.role],
                    contributedCents = it[AccountMembers.contributedCents]
                )
            }
        AccountDto(
            id = row[Accounts.id].value.toString(),
            name = row[Accounts.name],
            balanceCents = row[Accounts.balanceCents],
            currency = row[Accounts.currency],
            ownerUserId = row[Accounts.ownerUserId].value.toString(),
            members = members,
            createdAt = row[Accounts.createdAt].toString()
        )
    }

    fun listForUser(userId: UUID): List<AccountDto> = dbQuery {
        val accountIds = AccountMembers
            .select { AccountMembers.userId eq userId }
            .map { it[AccountMembers.accountId].value }
        accountIds.mapNotNull { getById(it) }
    }

    fun isMember(accountId: UUID, userId: UUID): Boolean = dbQuery {
        AccountMembers.select {
            (AccountMembers.accountId eq accountId) and (AccountMembers.userId eq userId)
        }.any()
    }

    fun isOwner(accountId: UUID, userId: UUID): Boolean = dbQuery {
        Accounts.select { (Accounts.id eq accountId) and (Accounts.ownerUserId eq userId) }.any()
    }

    fun inviteMember(accountId: UUID, requesterId: UUID, email: String): MemberDto? {
        if (!isOwner(accountId, requesterId)) error("Only owner can invite")
        val emailNorm = email.trim().lowercase()
        return dbQuery {
            val userRow = Users.select { Users.email eq emailNorm }.singleOrNull()
                ?: error("User with email $emailNorm not found. They must register first.")
            val userId = userRow[Users.id].value
            val exists = AccountMembers.select {
                (AccountMembers.accountId eq accountId) and (AccountMembers.userId eq userId)
            }.any()
            if (exists) error("Already a member")
            AccountMembers.insert {
                it[this.accountId] = accountId
                it[this.userId] = userId
                it[role] = "member"
                it[joinedAt] = Instant.now()
            }
            MemberDto(
                userId = userId.toString(),
                email = userRow[Users.email],
                displayName = userRow[Users.displayName],
                role = "member",
                contributedCents = 0
            )
        }
    }

    fun requestInviteCode(accountId: UUID, requesterId: UUID, email: String): RequestInviteCodeResponse {
        if (!isOwner(accountId, requesterId)) error("Only owner can invite")

        val emailNorm = email.trim().lowercase()
        if (emailNorm.isBlank()) error("Email is required")

        val groupName = dbQuery {
            Accounts.select { Accounts.id eq accountId }.singleOrNull()?.get(Accounts.name)
        } ?: error("Account not found")

        val userRow = dbQuery {
            Users.select { Users.email eq emailNorm }.singleOrNull()
        } ?: error("User with email $emailNorm not found. They must register first.")

        val invitedUserId = userRow[Users.id].value

        val exists = dbQuery {
            AccountMembers.select {
                (AccountMembers.accountId eq accountId) and (AccountMembers.userId eq invitedUserId)
            }.any()
        }
        if (exists) error("Already a member")

        val code = generateCode()
        val payload = PendingFamilyInvite(
            accountId = accountId.toString(),
            requesterId = requesterId.toString(),
            email = emailNorm,
            code = code,
            createdAt = Instant.now().toString()
        )

        redis.setEx(
            inviteKey(accountId, emailNorm),
            10 * 60,
            json.encodeToString(payload)
        )

        emailService.sendVerificationCode(emailNorm, code)

        return RequestInviteCodeResponse(
            ok = true,
            message = "Verification code sent to $emailNorm for group \"$groupName\""
        )
    }

    fun confirmInviteCode(
        accountId: UUID,
        requesterId: UUID,
        email: String,
        code: String
    ): ConfirmInviteCodeResponse {
        if (!isOwner(accountId, requesterId)) error("Only owner can invite")

        val emailNorm = email.trim().lowercase()
        val stored = redis.get(inviteKey(accountId, emailNorm))
            ?: error("Code expired")

        val pending = json.decodeFromString(PendingFamilyInvite.serializer(), stored)

        if (pending.requesterId != requesterId.toString()) error("Invite request mismatch")
        if (pending.accountId != accountId.toString()) error("Invite request mismatch")

        if (pending.code != code.trim()) {
            val fails = redis.incrWithTtl(inviteFailKey(accountId, emailNorm), 15 * 60)
            if (fails > 5) {
                redis.del(inviteKey(accountId, emailNorm))
            }
            error("Invalid code")
        }

        redis.del(inviteKey(accountId, emailNorm))
        redis.del(inviteFailKey(accountId, emailNorm))

        val member = inviteMember(accountId, requesterId, emailNorm)
            ?: error("Failed to add member")

        return ConfirmInviteCodeResponse(
            ok = true,
            member = member
        )
    }

    fun addFunds(accountId: UUID, userId: UUID, amountCents: Long): AccountDto {
        if (amountCents <= 0) error("Amount must be positive")
        if (!isMember(accountId, userId)) error("Not a member")
        dbQuery {
            Accounts.update({ Accounts.id eq accountId }) {
                with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                    it.update(balanceCents, balanceCents + amountCents)
                }
            }
            AccountMembers.update({
                (AccountMembers.accountId eq accountId) and (AccountMembers.userId eq userId)
            }) {
                with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                    it.update(contributedCents, contributedCents + amountCents)
                }
            }
        }
        return getById(accountId)!!
    }

    private fun inviteKey(accountId: UUID, email: String) = "family-invite:$accountId:$email"

    private fun inviteFailKey(accountId: UUID, email: String) = "family-invite-fail:$accountId:$email"

    private fun generateCode(): String = Random.nextInt(100000, 999999).toString()
}