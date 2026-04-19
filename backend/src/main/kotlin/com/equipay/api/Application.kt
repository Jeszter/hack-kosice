package com.equipay.api

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.equipay.api.accounts.AccountService
import com.equipay.api.accounts.accountRoutes
import com.equipay.api.ai.AiClient
import com.equipay.api.ai.AiService
import com.equipay.api.ai.aiRoutes
import com.equipay.api.auth.AuthService
import com.equipay.api.auth.BadRequest
import com.equipay.api.auth.ErrorResponse
import com.equipay.api.auth.JwtService
import com.equipay.api.auth.authRoutes
import com.equipay.api.banks.BankService
import com.equipay.api.banks.TatraBankClient
import com.equipay.api.banks.bankRoutes
import com.equipay.api.cards.CardService
import com.equipay.api.cards.cardRoutes
import com.equipay.api.config.loadConfig
import com.equipay.api.db.DbFactory
import com.equipay.api.email.EmailServiceFactory
import com.equipay.api.redis.RedisClient
import com.equipay.api.transactions.TransactionService
import com.equipay.api.transactions.transactionRoutes
import com.equipay.api.users.UserService
import com.equipay.api.users.userRoutes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val config = loadConfig(environment)

    DbFactory.init(config.db)
    val redis = RedisClient(config.redis)
    val emailService = EmailServiceFactory.create(config.email)

    val jwtService = JwtService(config.jwt)
    val authService = AuthService(jwtService, redis, emailService)

    val userService = UserService()
    val accountService = AccountService(emailService, redis)
    val cardService = CardService(accountService)
    val tatraClient = TatraBankClient(config.tatraBank)
    val bankService = BankService(tatraClient, redis)
    val transactionService = TransactionService(accountService, tatraClient)
    val aiClient = AiClient(config.openRouter)
    val aiService = AiService(aiClient, accountService)

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                encodeDefaults = true
            }
        )
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
    }

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "equipay"
            verifier(
                JWT.require(Algorithm.HMAC256(config.jwt.secret))
                    .withIssuer(config.jwt.issuer)
                    .build()
            )
            validate { credential ->
                val type = credential.payload.getClaim("type").asString()
                if (type == "access" && credential.payload.subject != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Token invalid or expired"))
            }
        }
    }

    install(StatusPages) {
        exception<BadRequest> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Bad request"))
        }
        exception<Throwable> { call, cause ->
            this@module.environment.log.error("Unhandled", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal error"))
        }
    }

    routing {
        get("/health") {
            call.respondText("ok")
        }

        get("/") {
            call.respondText("EquiPay API")
        }

        authRoutes(authService)
        userRoutes(userService)
        accountRoutes(accountService, bankService)
        cardRoutes(cardService)
        bankRoutes(bankService)
        transactionRoutes(transactionService)
        aiRoutes(aiService)
    }
}