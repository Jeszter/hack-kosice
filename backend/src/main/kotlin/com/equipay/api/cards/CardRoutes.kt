package com.equipay.api.cards

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

private fun ApplicationCall.userIdFromJwt(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)

fun Route.cardRoutes(svc: CardService) {
    authenticate("auth-jwt") {
        route("/cards") {
            get {
                val userId = call.userIdFromJwt()
                call.respond(svc.listForUser(userId))
            }
            post {
                val userId = call.userIdFromJwt()
                val req = call.receive<CreateCardRequest>()
                try {
                    call.respond(HttpStatusCode.Created, svc.create(userId, req.accountId))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error")))
                }
            }

            post("/{id}/freeze") {
                val userId = call.userIdFromJwt()
                val cardId = UUID.fromString(call.parameters["id"]!!)
                val req = call.receive<FreezeCardRequest>()
                try {
                    call.respond(svc.freeze(cardId, userId, req.frozen))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error")))
                }
            }
        }

        get("/accounts/{id}/cards") {
            val accountId = UUID.fromString(call.parameters["id"]!!)
            call.respond(svc.listForAccount(accountId))
        }
    }
}
