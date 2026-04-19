package com.equipay.api.users

import com.equipay.api.db.Users
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
import java.util.UUID

@Serializable data class UserDto(
    val id: String,
    val email: String,
    val displayName: String?
)

@Serializable data class UpdateProfileRequest(val displayName: String?)

class UserService {
    fun findByEmail(email: String): UserDto? = dbQuery {
        Users.select { Users.email eq email.trim().lowercase() }
            .singleOrNull()
            ?.toDto()
    }

    fun getById(userId: UUID): UserDto? = dbQuery {
        Users.select { Users.id eq userId }.singleOrNull()?.toDto()
    }

    fun searchByEmailPrefix(prefix: String, limit: Int = 10): List<UserDto> = dbQuery {
        if (prefix.length < 2) return@dbQuery emptyList()
        Users.select { Users.email like "${prefix.trim().lowercase()}%" }
            .limit(limit)
            .map { it.toDto() }
    }

    fun updateProfile(userId: UUID, req: UpdateProfileRequest): UserDto {
        dbQuery {
            Users.update({ Users.id eq userId }) {
                it[displayName] = req.displayName
                it[updatedAt] = java.time.Instant.now()
            }
        }
        return getById(userId)!!
    }

    private fun ResultRow.toDto() = UserDto(
        id = this[Users.id].value.toString(),
        email = this[Users.email],
        displayName = this[Users.displayName]
    )
}

private fun ApplicationCall.userIdFromJwt(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)

fun Route.userRoutes(svc: UserService) {
    authenticate("auth-jwt") {
        route("/users") {
            get("/me") {
                val userId = call.userIdFromJwt()
                val user = svc.getById(userId) ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(user)
            }

            put("/me") {
                val userId = call.userIdFromJwt()
                val req = call.receive<UpdateProfileRequest>()
                call.respond(svc.updateProfile(userId, req))
            }

            get("/search") {
                val query = call.request.queryParameters["q"] ?: ""
                call.respond(svc.searchByEmailPrefix(query))
            }

            get("/by-email") {
                val email = call.request.queryParameters["email"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val user = svc.findByEmail(email)
                if (user != null) call.respond(user) else call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
