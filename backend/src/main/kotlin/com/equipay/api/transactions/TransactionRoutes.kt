package com.equipay.api.transactions

import com.equipay.api.db.AccountMembers
import com.equipay.api.db.Transactions
import com.equipay.api.db.dbQuery
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

@Serializable data class InsightsDto(
    val spendingThisWeekCents: Long,
    val deltaPercent: Double,
    val weeklySpend: List<DailySpendDto>,
    val topCategories: List<CategoryDto>
)

@Serializable data class DailySpendDto(val dayShort: String, val amountCents: Long)
@Serializable data class CategoryDto(val category: String, val amountCents: Long)

private fun ApplicationCall.userIdFromJwt(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)

fun Route.transactionRoutes(svc: TransactionService) {
    authenticate("auth-jwt") {
        route("/transactions") {
            get {
                val userId = call.userIdFromJwt()
                call.respond(svc.listForUser(userId))
            }

            post {
                val userId = call.userIdFromJwt()
                val req = call.receive<CreateTransactionRequest>()
                try {
                    call.respond(HttpStatusCode.Created, svc.create(userId, req))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error")))
                }
            }

            get("/{id}") {
                val txId = UUID.fromString(call.parameters["id"]!!)
                val tx = svc.getById(txId)
                if (tx != null) call.respond(tx) else call.respond(HttpStatusCode.NotFound)
            }
        }

        get("/accounts/{id}/transactions") {
            val accountId = UUID.fromString(call.parameters["id"]!!)
            call.respond(svc.listForAccount(accountId))
        }

        get("/insights") {
            val userId = call.userIdFromJwt()
            call.respond(computeInsights(userId))
        }
    }
}

private fun computeInsights(userId: UUID): InsightsDto = dbQuery {
    val now = Instant.now()
    val weekAgo = now.minus(7, ChronoUnit.DAYS)
    val twoWeeksAgo = now.minus(14, ChronoUnit.DAYS)

    // Находим счета юзера
    val accountIds = AccountMembers.select { AccountMembers.userId eq userId }
        .map { it[AccountMembers.accountId].value }
    if (accountIds.isEmpty()) {
        return@dbQuery InsightsDto(0, 0.0, emptyList(), emptyList())
    }

    val thisWeek = Transactions.select {
        (Transactions.accountId inList accountIds) and
        (Transactions.createdAt greaterEq weekAgo) and
        (Transactions.status eq "completed")
    }.toList()

    val lastWeek = Transactions.select {
        (Transactions.accountId inList accountIds) and
        (Transactions.createdAt greaterEq twoWeeksAgo) and
        (Transactions.createdAt less weekAgo) and
        (Transactions.status eq "completed")
    }.toList()

    val thisWeekTotal = thisWeek.sumOf { it[Transactions.totalCents] }
    val lastWeekTotal = lastWeek.sumOf { it[Transactions.totalCents] }

    val delta = if (lastWeekTotal > 0) {
        ((thisWeekTotal - lastWeekTotal).toDouble() / lastWeekTotal * 100.0)
    } else 0.0

    // Weekly breakdown (последние 7 дней, по дням недели)
    val dayFormat = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val perDay = LongArray(7)
    thisWeek.forEach { tx ->
        val day = tx[Transactions.createdAt].atZone(ZoneOffset.UTC).dayOfWeek.value % 7  // 0=Sun..6=Sat
        perDay[day] += tx[Transactions.totalCents]
    }
    val weeklySpend = (1..6).map { DailySpendDto(dayFormat[it], perDay[it]) } +
                      DailySpendDto(dayFormat[0], perDay[0])

    // Top categories
    val byCategory = thisWeek
        .groupBy { it[Transactions.category] ?: "Other" }
        .mapValues { it.value.sumOf { tx -> tx[Transactions.totalCents] } }
        .entries.sortedByDescending { it.value }
        .take(5)
        .map { CategoryDto(it.key, it.value) }

    InsightsDto(
        spendingThisWeekCents = thisWeekTotal,
        deltaPercent = delta,
        weeklySpend = weeklySpend,
        topCategories = byCategory
    )
}
