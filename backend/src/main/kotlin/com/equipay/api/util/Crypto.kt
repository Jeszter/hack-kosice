package com.equipay.api.util

import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest

object PasswordHasher {
    fun hash(plain: String): String = BCrypt.hashpw(plain, BCrypt.gensalt(12))
    fun verify(plain: String, hash: String): Boolean = try {
        BCrypt.checkpw(plain, hash)
    } catch (e: Exception) {
        false
    }
}

object Sha256 {
    private val md = MessageDigest.getInstance("SHA-256")

    /** SHA-256 hex, used only for refresh token storage (lookup key). */
    fun hex(input: String): String {
        val bytes = synchronized(md) {
            md.reset()
            md.digest(input.toByteArray(Charsets.UTF_8))
        }
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
