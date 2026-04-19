package com.equipay.api.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.authRoutes(service: AuthService) {
    route("/auth") {
        post("/register") {
            val req = call.receive<RegisterRequest>()
            service.register(req).fold(
                onSuccess = { call.respond(HttpStatusCode.Created, it) },
                onFailure = { call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "Error")) }
            )
        }

        post("/verify-email") {
            val req = call.receive<VerifyEmailRequest>()
            service.verifyEmail(req).fold(
                onSuccess = { call.respond(HttpStatusCode.OK, it) },
                onFailure = { call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "Error")) }
            )
        }

        post("/resend-code") {
            val req = call.receive<ResendCodeRequest>()
            service.resendCode(req.email).fold(
                onSuccess = { call.respond(HttpStatusCode.OK, it) },
                onFailure = { call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "Error")) }
            )
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            val ip = call.request.local.remoteHost
            service.loginWithPassword(req, ip).fold(
                onSuccess = { call.respond(HttpStatusCode.OK, it) },
                onFailure = { call.respond(HttpStatusCode.Unauthorized, ErrorResponse(it.message ?: "Error")) }
            )
        }

        post("/login-pin") {
            val req = call.receive<LoginWithPinRequest>()
            service.loginWithPin(req).fold(
                onSuccess = { call.respond(HttpStatusCode.OK, it) },
                onFailure = { call.respond(HttpStatusCode.Unauthorized, ErrorResponse(it.message ?: "Error")) }
            )
        }

        post("/refresh") {
            val req = call.receive<RefreshRequest>()
            service.refresh(req.refreshToken).fold(
                onSuccess = { call.respond(HttpStatusCode.OK, it) },
                onFailure = { call.respond(HttpStatusCode.Unauthorized, ErrorResponse(it.message ?: "Error")) }
            )
        }

        post("/logout") {
            val req = call.receive<RefreshRequest>()
            service.logout(req.refreshToken)
            call.respond(HttpStatusCode.OK, OkResponse(message = "Logged out"))
        }

        authenticate("auth-jwt") {
            post("/pin") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = UUID.fromString(principal.payload.subject)
                val req = call.receive<SetPinRequest>()
                service.setPin(userId, req.pin).fold(
                    onSuccess = { call.respond(HttpStatusCode.OK, it) },
                    onFailure = { call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "Error")) }
                )
            }

            get("/me") {
                val principal = call.principal<JWTPrincipal>()!!
                call.respond(
                    mapOf(
                        "userId" to principal.payload.subject,
                        "email" to principal.payload.getClaim("email").asString()
                    )
                )
            }
        }
    }
}