package com.equipay.api.auth

import com.equipay.api.db.AuditLog
import com.equipay.api.db.RefreshTokens
import com.equipay.api.db.Users
import com.equipay.api.db.dbQuery
import com.equipay.api.email.EmailService
import com.equipay.api.redis.RedisClient
import com.equipay.api.util.PasswordHasher
import com.equipay.api.util.Sha256
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID
import kotlin.random.Random
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

sealed class AuthResult {
    data class Success(val tokens: TokenResponse) : AuthResult()
    data class Failure(val code: String, val message: String) : AuthResult()
}

class AuthService(
    private val jwt: JwtService,
    private val redis: RedisClient,
    private val email: EmailService
) {
    fun register(req: RegisterRequest): Result<RegisterResponse> {
        val emailNorm = req.email.trim().lowercase()
        if (!emailNorm.matches(EMAIL_RE)) return Result.failure(BadRequest("Invalid email"))
        if (req.password.length < 8) return Result.failure(BadRequest("Password must be at least 8 characters"))

        val exists = dbQuery {
            Users.selectAll().where { Users.email eq emailNorm }.any()
        }
        if (exists) return Result.failure(BadRequest("Email already registered"))

        val now = Instant.now()
        val hash = PasswordHasher.hash(req.password)

        val userId = dbQuery {
            Users.insertAndGetId {
                it[email] = emailNorm
                it[passwordHash] = hash
                it[displayName] = req.displayName
                it[emailVerified] = false
                it[createdAt] = now
                it[updatedAt] = now
            }.value
        }

        val code = generateCode()
        redis.setEx(codeKey(emailNorm), 600, code)
        email.sendVerificationCode(emailNorm, code)

        logAudit(userId, "USER_REGISTERED", mapOf("email" to emailNorm))

        return Result.success(
            RegisterResponse(
                userId = userId.toString(),
                email = emailNorm
            )
        )
    }

    fun resendCode(email: String): Result<OkResponse> {
        val emailNorm = email.trim().lowercase()

        val userRow = dbQuery {
            Users.selectAll().where { Users.email eq emailNorm }.singleOrNull()
        } ?: return Result.failure(BadRequest("User not found"))

        val rlKey = "rl:resend:$emailNorm"
        val count = redis.incrWithTtl(rlKey, 3600)
        if (count > 3) return Result.failure(BadRequest("Too many requests, try later"))

        val code = generateCode()
        redis.setEx(codeKey(emailNorm), 600, code)
        this.email.sendVerificationCode(emailNorm, code)

        logAudit(userRow[Users.id].value, "CODE_RESEND", null)

        return Result.success(OkResponse(message = "Code resent"))
    }

    fun verifyEmail(req: VerifyEmailRequest): Result<TokenResponse> {
        val emailNorm = req.email.trim().lowercase()

        val expected = redis.get(codeKey(emailNorm))
            ?: return Result.failure(BadRequest("Code expired"))

        if (expected != req.code.trim()) {
            val failKey = "rl:code_fail:$emailNorm"
            val fails = redis.incrWithTtl(failKey, 900)
            if (fails > 5) {
                redis.del(codeKey(emailNorm))
            }
            return Result.failure(BadRequest("Invalid code"))
        }

        redis.del(codeKey(emailNorm))

        val userRow = dbQuery {
            Users.selectAll().where { Users.email eq emailNorm }.singleOrNull()
        } ?: return Result.failure(BadRequest("User not found"))

        val userId = userRow[Users.id].value

        dbQuery {
            Users.update({ Users.id eq userId }) {
                it[emailVerified] = true
                it[updatedAt] = Instant.now()
            }
        }

        logAudit(userId, "EMAIL_VERIFIED", null)

        return Result.success(
            issueTokens(
                userId = userId,
                email = emailNorm,
                hasPin = userRow[Users.pinHash] != null,
                deviceId = req.deviceId,
                deviceName = req.deviceName
            )
        )
    }

    fun loginWithPassword(req: LoginRequest, ipAddress: String?): Result<TokenResponse> {
        val emailNorm = req.email.trim().lowercase()
        val rlKey = "rl:login:$emailNorm"
        val attempts = redis.incrWithTtl(rlKey, 900)

        if (attempts > 10) {
            return Result.failure(BadRequest("Too many attempts. Try again in 15 minutes."))
        }

        val userRow = dbQuery {
            Users.selectAll().where { Users.email eq emailNorm }.singleOrNull()
        }

        if (userRow == null || !PasswordHasher.verify(req.password, userRow[Users.passwordHash])) {
            logAudit(
                userId = userRow?.get(Users.id)?.value,
                eventType = "AUTH_LOGIN_FAIL",
                metadata = mapOf(
                    "email" to emailNorm,
                    "ip" to (ipAddress ?: "-"),
                    "method" to "password"
                )
            )
            return Result.failure(BadRequest("Invalid email or password"))
        }

        if (!userRow[Users.emailVerified]) {
            return Result.failure(BadRequest("Email not verified"))
        }

        val userId = userRow[Users.id].value
        val hasPin = userRow[Users.pinHash] != null

        logAudit(
            userId = userId,
            eventType = "AUTH_LOGIN",
            metadata = mapOf(
                "method" to "password",
                "hasPin" to hasPin.toString()
            )
        )

        return Result.success(
            issueTokens(
                userId = userId,
                email = emailNorm,
                hasPin = hasPin,
                deviceId = req.deviceId,
                deviceName = req.deviceName
            )
        )
    }

    fun loginWithPin(req: LoginWithPinRequest): Result<TokenResponse> {
        val emailNorm = req.email.trim().lowercase()
        val rlKey = "rl:pin:$emailNorm"
        val attempts = redis.incrWithTtl(rlKey, 900)

        if (attempts > 5) {
            return Result.failure(BadRequest("Too many PIN attempts. Use email login."))
        }

        val userRow = dbQuery {
            Users.selectAll().where { Users.email eq emailNorm }.singleOrNull()
        } ?: return Result.failure(BadRequest("Invalid credentials"))

        if (!userRow[Users.emailVerified]) {
            return Result.failure(BadRequest("Email not verified"))
        }

        val pinHash = userRow[Users.pinHash]
            ?: return Result.failure(BadRequest("PIN not set"))

        if (!PasswordHasher.verify(req.pin, pinHash)) {
            logAudit(
                userId = userRow[Users.id].value,
                eventType = "PIN_FAIL",
                metadata = mapOf("method" to "pin")
            )
            return Result.failure(BadRequest("Invalid PIN"))
        }

        val userId = userRow[Users.id].value

        logAudit(
            userId = userId,
            eventType = "AUTH_LOGIN",
            metadata = mapOf("method" to "pin")
        )

        return Result.success(
            issueTokens(
                userId = userId,
                email = emailNorm,
                hasPin = true,
                deviceId = req.deviceId,
                deviceName = req.deviceName
            )
        )
    }

    fun setPin(userId: UUID, pin: String): Result<OkResponse> {
        if (!pin.matches(Regex("^\\d{4,6}$"))) {
            return Result.failure(BadRequest("PIN must be 4-6 digits"))
        }

        val hash = PasswordHasher.hash(pin)

        dbQuery {
            Users.update({ Users.id eq userId }) {
                it[pinHash] = hash
                it[updatedAt] = Instant.now()
            }
        }

        logAudit(userId, "PIN_SET", null)

        return Result.success(OkResponse(message = "PIN set"))
    }

    fun refresh(refreshToken: String): Result<TokenResponse> {
        val decoded = jwt.verify(refreshToken)
            ?: return Result.failure(BadRequest("Invalid refresh token"))

        if (decoded.getClaim("type").asString() != "refresh") {
            return Result.failure(BadRequest("Wrong token type"))
        }

        val hash = Sha256.hex(refreshToken)
        val now = Instant.now()

        val session = dbQuery {
            val row = RefreshTokens.selectAll().where { RefreshTokens.tokenHash eq hash }.singleOrNull()
                ?: return@dbQuery null

            if (row[RefreshTokens.revokedAt] != null) return@dbQuery null
            if (row[RefreshTokens.expiresAt].isBefore(now)) return@dbQuery null

            RefreshTokens.update({ RefreshTokens.tokenHash eq hash }) {
                it[revokedAt] = now
            }

            Triple(
                row[RefreshTokens.userId].value,
                row[RefreshTokens.deviceId],
                row[RefreshTokens.deviceName]
            )
        } ?: return Result.failure(BadRequest("Invalid or expired refresh token"))

        val userRow = dbQuery {
            Users.selectAll().where { Users.id eq session.first }.single()
        }

        return Result.success(
            issueTokens(
                userId = session.first,
                email = userRow[Users.email],
                hasPin = userRow[Users.pinHash] != null,
                deviceId = session.second,
                deviceName = session.third
            )
        )
    }

    fun logout(refreshToken: String) {
        val hash = Sha256.hex(refreshToken)

        dbQuery {
            RefreshTokens.update({ RefreshTokens.tokenHash eq hash }) {
                it[revokedAt] = Instant.now()
            }
        }
    }

    private fun issueTokens(
        userId: UUID,
        email: String,
        hasPin: Boolean,
        deviceId: String?,
        deviceName: String?
    ): TokenResponse {
        val (access, accessExp) = jwt.issueAccessToken(userId, email)
        val (refresh, refreshExp) = jwt.issueRefreshToken(userId)
        val refreshHash = Sha256.hex(refresh)

        dbQuery {
            RefreshTokens.insert {
                it[RefreshTokens.userId] = userId
                it[tokenHash] = refreshHash
                it[RefreshTokens.deviceId] = deviceId
                it[RefreshTokens.deviceName] = deviceName
                it[expiresAt] = refreshExp
                it[createdAt] = Instant.now()
            }
        }

        return TokenResponse(
            accessToken = access,
            refreshToken = refresh,
            accessExpiresAt = accessExp.toString(),
            userId = userId.toString(),
            email = email,
            hasPin = hasPin
        )
    }

    private fun logAudit(userId: UUID?, eventType: String, metadata: Map<String, String>?) {
        try {
            dbQuery {
                AuditLog.insert {
                    it[AuditLog.userId] = userId
                    it[AuditLog.eventType] = eventType
                    it[AuditLog.metadata] = metadata?.entries
                        ?.joinToString(",", "{", "}") { e ->
                            "\"${e.key}\":\"${e.value}\""
                        }
                    it[createdAt] = Instant.now()
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun codeKey(email: String) = "email_code:$email"

    private fun generateCode(): String = "%06d".format(Random.nextInt(0, 1_000_000))

    companion object {
        private val EMAIL_RE = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}

class BadRequest(message: String) : RuntimeException(message)