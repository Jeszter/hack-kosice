package com.equipay.api.cards

import com.equipay.api.accounts.AccountService
import com.equipay.api.db.VirtualCards
import com.equipay.api.db.dbQuery
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

// ========= DTO =========
@Serializable data class VirtualCardDto(
    val id: String,
    val accountId: String,
    val userId: String,
    val last4: String,
    val provider: String,
    val active: Boolean,
    val frozen: Boolean,
    val createdAt: String
)

@Serializable data class CreateCardRequest(val accountId: String)
@Serializable data class FreezeCardRequest(val frozen: Boolean)

// ========= Service =========

class CardService(private val accountService: AccountService) {

    fun create(userId: UUID, accountIdRaw: String): VirtualCardDto {
        val accountId = UUID.fromString(accountIdRaw)
        if (!accountService.isMember(accountId, userId)) error("Not a member of this account")

        // Mock PAN generation (TODO(security): в проде — интеграция с card issuer, напр. Marqeta)
        val last4 = "%04d".format(Random.nextInt(10000))
        val externalId = "mock_${UUID.randomUUID().toString().take(8)}"

        val id = dbQuery {
            VirtualCards.insertAndGetId {
                it[VirtualCards.accountId] = accountId
                it[VirtualCards.userId] = userId
                it[VirtualCards.last4] = last4
                it[provider] = "internal_mock"
                it[externalCardId] = externalId
                it[active] = true
                it[frozen] = false
                it[createdAt] = Instant.now()
            }.value
        }
        return getById(id)!!
    }

    fun getById(cardId: UUID): VirtualCardDto? = dbQuery {
        VirtualCards.select { VirtualCards.id eq cardId }.singleOrNull()?.toDto()
    }

    fun listForUser(userId: UUID): List<VirtualCardDto> = dbQuery {
        VirtualCards.select { VirtualCards.userId eq userId }
            .orderBy(VirtualCards.createdAt to SortOrder.DESC)
            .map { it.toDto() }
    }

    fun listForAccount(accountId: UUID): List<VirtualCardDto> = dbQuery {
        VirtualCards.select { VirtualCards.accountId eq accountId }
            .orderBy(VirtualCards.createdAt to SortOrder.DESC)
            .map { it.toDto() }
    }

    fun freeze(cardId: UUID, userId: UUID, frozen: Boolean): VirtualCardDto {
        dbQuery {
            val row = VirtualCards.select { VirtualCards.id eq cardId }.singleOrNull()
                ?: error("Card not found")
            if (row[VirtualCards.userId].value != userId) error("Not your card")
            VirtualCards.update({ VirtualCards.id eq cardId }) {
                it[VirtualCards.frozen] = frozen
            }
        }
        return getById(cardId)!!
    }

    private fun ResultRow.toDto() = VirtualCardDto(
        id = this[VirtualCards.id].value.toString(),
        accountId = this[VirtualCards.accountId].value.toString(),
        userId = this[VirtualCards.userId].value.toString(),
        last4 = this[VirtualCards.last4],
        provider = this[VirtualCards.provider],
        active = this[VirtualCards.active],
        frozen = this[VirtualCards.frozen],
        createdAt = this[VirtualCards.createdAt].toString()
    )
}
