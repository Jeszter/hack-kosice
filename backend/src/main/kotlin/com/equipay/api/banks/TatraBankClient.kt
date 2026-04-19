package com.equipay.api.banks

import com.equipay.api.config.TatraBankConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDate
import java.util.Base64
import java.util.UUID

class TatraBankClient(private val cfg: TatraBankConfig) {
    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(CIO) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(json)
        }
    }

    @Serializable
    data class OAuthTokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("token_type") val tokenType: String? = null,
        @SerialName("expires_in") val expiresIn: Long? = null,
        @SerialName("refresh_token") val refreshToken: String? = null,
        val scope: String? = null
    )

    @Serializable
    data class ConsentResponse(
        @SerialName("consentId") val consentId: String
    )

    @Serializable
    data class AccountListResponse(
        val accounts: List<TatraAccount> = emptyList()
    )

    @Serializable
    data class TatraAccount(
        val resourceId: String,
        val iban: String? = null,
        val currency: String? = null,
        val name: String? = null,
        val balances: List<TatraBalance> = emptyList()
    )

    @Serializable
    data class TatraBalance(
        val balanceAmount: TatraAmount? = null,
        val balanceType: String? = null
    )

    @Serializable
    data class TatraAmount(
        val amount: String? = null,
        val currency: String? = null
    )

    @Serializable
    data class PaymentResult(
        val paymentId: String,
        val status: String
    )

    data class PkcePair(
        val verifier: String,
        val challenge: String
    )

    private fun newRequestId(): String = UUID.randomUUID().toString()

    fun createPkcePair(): PkcePair {
        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        val verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(random)
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(StandardCharsets.UTF_8))
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        return PkcePair(verifier, challenge)
    }

    suspend fun getClientCredentialsToken(): OAuthTokenResponse {
        val url = "${cfg.baseUrl}/auth/oauth/v2/token"
        println("Tatra token URL = $url")

        val response = client.submitForm(
            url = url,
            formParameters = Parameters.build {
                append("grant_type", "client_credentials")
                append("client_id", cfg.clientId)
                append("client_secret", cfg.clientSecret)
                append("scope", "AIS")
            }
        )

        val body = response.bodyAsText()

        if (response.status.value !in 200..299) {
            error("Tatra token error: url=$url, status=${response.status.value}, body=$body")
        }

        if (body.isBlank()) {
            error(
                "Tatra token error: url=$url, empty response body. " +
                        "Check TATRA_CLIENT_ID, TATRA_CLIENT_SECRET and TATRA_BASE_URL."
            )
        }

        return json.decodeFromString(OAuthTokenResponse.serializer(), body)
    }

    suspend fun createConsent(clientAccessToken: String): ConsentResponse {
        val url = "${cfg.baseUrl}/v1/consents"
        val requestId = newRequestId()

        println("Tatra consent URL = $url")
        println("Tatra X-Request-ID = $requestId")

        val response = client.post(url) {
            header("Authorization", "Bearer $clientAccessToken")
            header("Content-Type", "application/json")
            header("X-Request-ID", requestId)
            header("PSU-IP-Address", "127.0.0.1")
            setBody(
                buildJsonObject {
                    put("access", buildJsonObject {
                        put("accounts", JsonArray(emptyList()))
                    })
                    put("recurringIndicator", true)
                    put("validUntil", LocalDate.now().plusDays(90).toString())
                    put("frequencyPerDay", 4)
                }
            )
        }

        val body = response.bodyAsText()

        if (response.status.value !in 200..299) {
            error("Tatra consent error: url=$url, requestId=$requestId, status=${response.status.value}, body=$body")
        }

        if (body.isBlank()) {
            error("Tatra consent error: url=$url, requestId=$requestId, empty response body")
        }

        return json.decodeFromString(ConsentResponse.serializer(), body)
    }

    fun buildAuthorizeUrl(consentId: String, state: String, codeChallenge: String): String {
        return URLBuilder("${cfg.baseUrl}/auth/oauth/v2/authorize").apply {
            parameters.append("client_id", cfg.clientId)
            parameters.append("scope", "AIS:$consentId")
            parameters.append("response_type", "code")
            parameters.append("redirect_uri", cfg.redirectUri)
            parameters.append("state", state)
            parameters.append("code_challenge", codeChallenge)
            parameters.append("code_challenge_method", "S256")
        }.buildString()
    }

    suspend fun exchangeCodeForToken(code: String, codeVerifier: String): OAuthTokenResponse {
        val url = "${cfg.baseUrl}/auth/oauth/v2/token"

        val response = client.submitForm(
            url = url,
            formParameters = Parameters.build {
                append("grant_type", "authorization_code")
                append("client_id", cfg.clientId)
                append("client_secret", cfg.clientSecret)
                append("redirect_uri", cfg.redirectUri)
                append("code", code)
                append("code_verifier", codeVerifier)
            }
        )

        val body = response.bodyAsText()

        if (response.status.value !in 200..299) {
            error("Tatra exchange code error: url=$url, status=${response.status.value}, body=$body")
        }

        if (body.isBlank()) {
            error("Tatra exchange code error: url=$url, empty response body")
        }

        return json.decodeFromString(OAuthTokenResponse.serializer(), body)
    }

    suspend fun readAccounts(userAccessToken: String, consentId: String): AccountListResponse {
        val url = "${cfg.baseUrl}/v1/accounts"
        val requestId = newRequestId()

        println("Tatra accounts URL = $url")
        println("Tatra accounts X-Request-ID = $requestId")
        println("Tatra accounts Consent-ID = $consentId")

        val response = client.get(url) {
            header("Authorization", "Bearer $userAccessToken")
            header("X-Request-ID", requestId)
            header("PSU-IP-Address", "127.0.0.1")
            header("Consent-ID", consentId)
            parameter("withBalance", true)
        }

        val body = response.bodyAsText()

        if (response.status.value !in 200..299) {
            error("Tatra accounts error: url=$url, requestId=$requestId, consentId=$consentId, status=${response.status.value}, body=$body")
        }

        if (body.isBlank()) {
            error("Tatra accounts error: url=$url, requestId=$requestId, consentId=$consentId, empty response body")
        }

        return json.decodeFromString(AccountListResponse.serializer(), body)
    }

    fun initiatePayment(
        debtorIban: String,
        creditorIban: String,
        amountCents: Long,
        remittance: String,
        consentToken: String
    ): PaymentResult {
        return PaymentResult(
            paymentId = "stub-payment-id",
            status = "RCVD"
        )
    }
}