package com.equipay.app.auth

import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory access token. Не персистим — access живёт 15 мин,
 * при рестарте приложения мы рефрешим его через refresh token из TokenStore.
 */
object AccessTokenHolder {
    private val token = AtomicReference<String?>(null)

    fun set(newToken: String?) {
        token.set(newToken)
    }

    fun get(): String? = token.get()

    fun bearer(): String? = token.get()?.let { "Bearer $it" }

    fun clear() {
        token.set(null)
    }
}
