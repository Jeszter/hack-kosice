package com.equipay.app.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Защищённое хранилище токенов (AES-256 + AndroidKeyStore под капотом).
 * Refresh-токен и email хранятся ТОЛЬКО тут. Access-токен держим в памяти.
 */
class TokenStore(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "equipay_secure_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSession(
        refreshToken: String,
        email: String,
        userId: String,
        hasPin: Boolean
    ) {
        prefs.edit()
            .putString(KEY_REFRESH, refreshToken)
            .putString(KEY_EMAIL, email)
            .putString(KEY_USER_ID, userId)
            .putBoolean(KEY_HAS_PIN, hasPin)
            .apply()
    }

    fun getRefresh(): String? = prefs.getString(KEY_REFRESH, null)
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun hasPin(): Boolean = prefs.getBoolean(KEY_HAS_PIN, false)

    fun setHasPin(has: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_PIN, has).apply()
    }

    fun isLoggedIn(): Boolean = getRefresh() != null

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EMAIL = "email"
        const val KEY_USER_ID = "user_id"
        const val KEY_HAS_PIN = "has_pin"
    }
}
