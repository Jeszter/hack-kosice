package com.equipay.api.accounts

import com.equipay.api.banks.BankService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

private fun ApplicationCall.userIdFromJwt(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)

fun Route.accountRoutes(svc: AccountService, bankService: BankService) {
    authenticate("auth-jwt") {
        route("/accounts") {
            get {
                val userId = call.userIdFromJwt()
                call.respond(svc.listForUser(userId))
            }

            post {
                val userId = call.userIdFromJwt()
                val req = call.receive<CreateAccountRequest>()
                if (req.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Name is required"))
                    return@post
                }
                call.respond(HttpStatusCode.Created, svc.create(userId, req))
            }

            route("/{id}") {
                get {
                    val userId = call.userIdFromJwt()
                    val accountId = UUID.fromString(call.parameters["id"]!!)
                    if (!svc.isMember(accountId, userId)) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member"))
                        return@get
                    }
                    val acc = svc.getById(accountId)
                    if (acc != null) call.respond(acc)
                    else call.respond(HttpStatusCode.NotFound)
                }

                get("/linked-balance") {
                    val userId = call.userIdFromJwt()
                    val accountId = UUID.fromString(call.parameters["id"]!!)

                    try {
                        val result = bankService.getLinkedBalanceForAccount(accountId, userId)
                        call.respond(result)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to (e.message ?: "Error"))
                        )
                    }
                }

                post("/members/request-invite") {
                    val userId = call.userIdFromJwt()
                    val accountId = UUID.fromString(call.parameters["id"]!!)
                    val req = call.receive<RequestInviteCodeRequest>()

                    try {
                        val result = svc.requestInviteCode(accountId, userId, req.email)
                        call.respond(HttpStatusCode.OK, result)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error")))
                    }
                }

                post("/members/confirm-invite") {
                    val userId = call.userIdFromJwt()
                    val accountId = UUID.fromString(call.parameters["id"]!!)
                    val req = call.receive<ConfirmInviteCodeRequest>()

                    try {
                        val result = svc.confirmInviteCode(accountId, userId, req.email, req.code)
                        call.respond(HttpStatusCode.OK, result)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error")))
                    }
                }

                post("/members") {
                    val userId = call.userIdFromJwt()
                    val accountId = UUID.fromString(call.parameters["id"]!!)
                    val req = call.receive<InviteMemberRequest>()
                    try {
                        val member = svc.inviteMember(accountId, userId, req.email)
                        call.respond(HttpStatusCode.Created, member!!)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error")))
                    }
                }

                post("/add-funds") {
                    val userId = call.userIdFromJwt()
                    val accountId = UUID.fromString(call.parameters["id"]!!)
                    val req = call.receive<AddFundsRequest>()
                    try {
                        val updated = svc.addFunds(accountId, userId, req.amountCents)
                        call.respond(updated)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error")))
                    }
                }
            }
        }
    }
}