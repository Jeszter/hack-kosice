package com.equipay.api.ai

import com.equipay.api.accounts.AccountService
import com.equipay.api.db.Accounts
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.jetbrains.exposed.sql.*
import java.util.UUID

// ========= DTO =========

@Serializable data class VoiceParseRequest(val transcript: String, val accountId: String? = null)

@Serializable data class VoiceParseResponse(
    val merchant: String,
    val amountCents: Long,
    val splitMode: String,
    val category: String? = null,
    val confidence: Double = 0.8,
    val confirmationText: String
)

@Serializable data class ReceiptParseRequest(val imageBase64: String, val mimeType: String = "image/jpeg", val accountId: String? = null)

@Serializable data class SmartSplitRequest(val accountId: String, val totalCents: Long, val merchant: String? = null)

@Serializable data class SmartSplitItem(val userId: String, val amountCents: Long)

@Serializable data class SmartSplitResponse(
    val mode: String,                // "equal" / "smart"
    val split: List<SmartSplitItem>,
    val suggestion: String
)

@Serializable data class RebalanceSuggestionDto(val message: String)

// ========= Service =========

class AiService(private val ai: AiClient, private val accounts: AccountService) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Парсит произвольный transcript в структурированный payment.
     * Промпт: объясняем сколько и как split.
     */
    suspend fun parseVoice(userId: UUID, transcript: String): VoiceParseResponse {
        val system = """
            You are EquiPay assistant. User describes a payment in natural language (any language).
            Extract: merchant (short name), amount in EUR cents (integer), split mode ("equal" if "split between us/everyone", "solo" if "only I paid"), category.
            Categories: Food, Transport, Shopping, Entertainment, Other.
            Return STRICT JSON: {"merchant":"...","amountCents":1000,"splitMode":"equal","category":"Food"}
        """.trimIndent()

        val raw = ai.chat(
            listOf(
                ChatMessage("system", system),
                ChatMessage("user", transcript)
            ),
            forceJson = true
        ) ?: return fallbackVoiceParse(transcript)

        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val merchant = obj["merchant"]?.jsonPrimitive?.content ?: "Purchase"
            val amount = obj["amountCents"]?.jsonPrimitive?.longOrNull ?: 0L
            val mode = obj["splitMode"]?.jsonPrimitive?.content ?: "equal"
            val cat = obj["category"]?.jsonPrimitive?.content
            val eur = String.format("%.2f", amount / 100.0)
            VoiceParseResponse(
                merchant = merchant,
                amountCents = amount,
                splitMode = mode,
                category = cat,
                confirmationText = "Got it! I'll split €$eur $mode for $merchant. Shall I proceed?"
            )
        } catch (e: Exception) {
            fallbackVoiceParse(transcript)
        }
    }

    /**
     * Vision: извлекает сумму/продавца/категорию из фото чека.
     * Использует Gemini через OpenRouter в multimodal режиме.
     */
    suspend fun parseReceipt(userId: UUID, imageBase64: String, mimeType: String): VoiceParseResponse {
        val system = """
            You are EquiPay receipt scanner. The user sends a photo of a receipt, bill, or payment confirmation.
            Extract:
              - merchant: short business name (e.g. "Pizza Place", "Uber", "SuperMarket")
              - amountCents: the TOTAL amount, as integer in EUR cents (e.g. €12.34 -> 1234). Find the grand total, not subtotal.
              - category: one of "Food", "Transport", "Shopping", "Entertainment", "Other"
              - splitMode: always "equal" for receipts (assume it's shared)
            Return STRICT JSON: {"merchant":"...","amountCents":1234,"splitMode":"equal","category":"Food"}
            If you cannot read the receipt, return {"merchant":"Unknown","amountCents":0,"splitMode":"equal","category":"Other"}.
        """.trimIndent()

        val raw = ai.chatWithImage(
            systemPrompt = system,
            userText = "Extract the total amount and merchant from this receipt.",
            imageBase64 = imageBase64,
            mimeType = mimeType,
            forceJson = true
        ) ?: return VoiceParseResponse(
            merchant = "Receipt",
            amountCents = 0,
            splitMode = "equal",
            confidence = 0.2,
            confirmationText = "Couldn't scan the receipt. Please type the amount manually."
        )

        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val merchant = obj["merchant"]?.jsonPrimitive?.content ?: "Receipt"
            val amount = obj["amountCents"]?.jsonPrimitive?.longOrNull ?: 0L
            val mode = obj["splitMode"]?.jsonPrimitive?.content ?: "equal"
            val cat = obj["category"]?.jsonPrimitive?.content
            val eur = String.format("%.2f", amount / 100.0)
            VoiceParseResponse(
                merchant = merchant,
                amountCents = amount,
                splitMode = mode,
                category = cat,
                confidence = if (amount > 0) 0.85 else 0.3,
                confirmationText = if (amount > 0)
                    "Got it! €$eur at $merchant. Split $mode — shall I proceed?"
                else
                    "Found $merchant but couldn't read the total. Please enter the amount."
            )
        } catch (e: Exception) {
            VoiceParseResponse(
                merchant = "Receipt",
                amountCents = 0,
                splitMode = "equal",
                confidence = 0.2,
                confirmationText = "Couldn't read the receipt clearly. Please type the amount."
            )
        }
    }

    private fun fallbackVoiceParse(transcript: String): VoiceParseResponse {
        // regex-бэкап на случай если AI недоступен
        val amountRegex = Regex("""(\d+(?:[.,]\d{1,2})?)\s*(?:eur|euro|€)""", RegexOption.IGNORE_CASE)
        val m = amountRegex.find(transcript)
        val amountCents = if (m != null) {
            val v = m.groupValues[1].replace(',', '.').toDouble()
            (v * 100).toLong()
        } else 0L
        return VoiceParseResponse(
            merchant = "Purchase",
            amountCents = amountCents,
            splitMode = if (transcript.contains("between", true) || transcript.contains("split", true)) "equal" else "solo",
            confidence = 0.4,
            confirmationText = "I understood €${amountCents / 100.0}. Please confirm."
        )
    }

    suspend fun smartSplit(userId: UUID, req: SmartSplitRequest): SmartSplitResponse {
        val accountId = UUID.fromString(req.accountId)
        val account = accounts.getById(accountId) ?: error("Account not found")
        val members = account.members

        // Простой вариант: посмотреть contributedCents — кто меньше отдал, тому меньше платить в этот раз
        // т.е. пытаемся выровнять вклад участников
        val total = req.totalCents
        val contribByUser = members.associate { it.userId to it.contributedCents }
        val maxContrib = contribByUser.values.max()

        // Вес = (maxContrib - contributed + 1) — кто меньше вложил, у того больший вес
        val weights = contribByUser.mapValues { (maxContrib - it.value + 1000) }
        val sumWeights = weights.values.sum().toDouble()

        val raw = weights.mapValues { ((it.value.toDouble() / sumWeights) * total).toLong() }
        // Фикс остатка от округления
        val currentSum = raw.values.sum()
        val diff = total - currentSum
        val first = raw.keys.first()
        val split = raw.toMutableMap().also { it[first] = it[first]!! + diff }
            .map { SmartSplitItem(it.key, it.value) }

        val suggestion = "Suggested a smart split based on past contributions. Members who contributed less pay more this time."

        return SmartSplitResponse(mode = "smart", split = split, suggestion = suggestion)
    }

    /** Короткий совет для Insights: типа "Yehor paid 70% this week". */
    fun rebalanceSuggestion(userId: UUID): RebalanceSuggestionDto = dbQuery {
        // Смотрим транзакции последней недели по всем счетам юзера
        val accountList = accounts.listForUser(userId)
        if (accountList.isEmpty()) return@dbQuery RebalanceSuggestionDto("Create a group to start splitting expenses.")
        val acc = accountList.first()
        val totalContributed = acc.members.sumOf { it.contributedCents }
        if (totalContributed == 0L) return@dbQuery RebalanceSuggestionDto("No payments yet. Invite friends and add funds!")

        val topContributor = acc.members.maxByOrNull { it.contributedCents }!!
        val share = (topContributor.contributedCents.toDouble() / totalContributed * 100).toInt()
        val name = topContributor.displayName ?: topContributor.email.substringBefore("@")
        RebalanceSuggestionDto(
            if (share > 50)
                "$name paid $share% this week. Want to rebalance next payment?"
            else
                "Payments are well-balanced across the group."
        )
    }
}

// ========= Routes =========

private fun ApplicationCall.userIdFromJwt(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)

fun Route.aiRoutes(svc: AiService) {
    authenticate("auth-jwt") {
        route("/ai") {
            post("/voice-parse") {
                val userId = call.userIdFromJwt()
                val req = call.receive<VoiceParseRequest>()
                call.respond(svc.parseVoice(userId, req.transcript))
            }

            post("/receipt-parse") {
                val userId = call.userIdFromJwt()
                val req = call.receive<ReceiptParseRequest>()
                if (req.imageBase64.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Image is required"))
                    return@post
                }
                call.respond(svc.parseReceipt(userId, req.imageBase64, req.mimeType))
            }

            post("/smart-split") {
                val userId = call.userIdFromJwt()
                val req = call.receive<SmartSplitRequest>()
                try {
                    call.respond(svc.smartSplit(userId, req))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error")))
                }
            }

            get("/insights-hint") {
                val userId = call.userIdFromJwt()
                call.respond(svc.rebalanceSuggestion(userId))
            }
        }
    }
}
