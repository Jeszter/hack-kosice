package com.equipay.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.equipay.api.config.JwtConfig
import java.time.Instant
import java.util.Date
import java.util.UUID

data class TokenPair(val accessToken: String, val refreshToken: String, val accessExpiresAt: Instant)

class JwtService(private val cfg: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(cfg.secret)
    val verifier = JWT.require(algorithm).withIssuer(cfg.issuer).build()

    fun issueAccessToken(userId: UUID, email: String): Pair<String, Instant> {
        val now = Instant.now()
        val exp = now.plusSeconds(cfg.accessTtlMin * 60)
        val token = JWT.create()
            .withIssuer(cfg.issuer)
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withClaim("type", "access")
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(exp))
            .sign(algorithm)
        return token to exp
    }

    /** Raw refresh token — returned to client once, stored only as hash in DB. */
    fun issueRefreshToken(userId: UUID): Pair<String, Instant> {
        val now = Instant.now()
        val exp = now.plusSeconds(cfg.refreshTtlDays * 86400)
        // Просто случайная 512-бит строка, подписанная тем же секретом.
        val token = JWT.create()
            .withIssuer(cfg.issuer)
            .withSubject(userId.toString())
            .withClaim("type", "refresh")
            .withClaim("jti", UUID.randomUUID().toString())
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(exp))
            .sign(algorithm)
        return token to exp
    }

    fun verify(token: String): DecodedJWT? = try {
        verifier.verify(token)
    } catch (e: Exception) {
        null
    }
}
