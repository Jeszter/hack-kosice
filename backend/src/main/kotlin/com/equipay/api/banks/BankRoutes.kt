package com.equipay.api.banks

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class BankErrorResponse(
    val error: String
)

private fun ApplicationCall.userIdFromJwt(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)

fun Route.bankRoutes(svc: BankService) {
    route("/banks") {
        authenticate("auth-jwt") {
            get("/available") {
                call.respond(svc.availableBanks())
            }

            get("/connections") {
                val userId = call.userIdFromJwt()
                call.respond(svc.listForUser(userId))
            }

            post("/tatra/connect/start") {
                val userId = call.userIdFromJwt()
                val result = svc.startTatraConnect(userId)
                call.respond(result)
            }

            delete("/connections/{id}") {
                val userId = call.userIdFromJwt()
                val id = UUID.fromString(call.parameters["id"]!!)
                svc.disconnect(userId, id)
                call.respond(HttpStatusCode.NoContent)
            }
        }

        get("/tatra/callback") {
            val code = call.request.queryParameters["code"]
                ?: return@get call.respondRedirect(
                    "equipay://tatra/callback?success=false&error=missing_code"
                )

            val state = call.request.queryParameters["state"]
                ?: return@get call.respondRedirect(
                    "equipay://tatra/callback?success=false&error=missing_state"
                )

            try {
                svc.finishTatraConnect(state, code)

                call.respondRedirect(
                    "equipay://tatra/callback?success=true"
                )
            } catch (e: Exception) {
                call.respondRedirect(
                    "equipay://tatra/callback?success=false&error=internal_error"
                )
            }
        }
    }
}