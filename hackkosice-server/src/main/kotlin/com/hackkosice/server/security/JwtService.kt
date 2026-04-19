package com.hackkosice.server.security

import com.hackkosice.server.entity.UserEntity
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    private val jwtProperties: JwtProperties
) {
    private fun signingKey(): SecretKey {
        val bytes = Decoders.BASE64.decode(encodeSecret(jwtProperties.secret))
        return Keys.hmacShaKeyFor(bytes)
    }

    private fun encodeSecret(raw: String): String {
        return java.util.Base64.getEncoder().encodeToString(raw.toByteArray())
    }

    fun generateToken(user: UserEntity): String {
        val now = Date()
        val expiry = Date(now.time + jwtProperties.expirationMs)

        return Jwts.builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("role", user.role.name)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey())
            .compact()
    }

    fun extractUserId(token: String): Long {
        return parseClaims(token).subject.toLong()
    }

    fun isTokenValid(token: String): Boolean {
        val claims = parseClaims(token)
        return claims.expiration.after(Date())
    }

    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
