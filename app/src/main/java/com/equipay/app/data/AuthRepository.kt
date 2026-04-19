package com.equipay.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.delay

/**
 * Простое хранилище учётных данных — в реальном продукте тут был бы
 * Tatra banka auth backend + EncryptedSharedPreferences + Keystore.
 * Сейчас это mock-слой, достаточный для демо приложения.
 */
class AuthRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------- USER ----------

    data class User(val email: String, val name: String)

    fun currentUser(): User? {
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val name = prefs.getString(KEY_NAME, "") ?: ""
        return User(email = email, name = name)
    }

    fun isSignedIn(): Boolean = prefs.getString(KEY_EMAIL, null) != null

    suspend fun signUp(name: String, email: String, password: String): Result<User> {
        delay(900) // имитация сети
        if (email.isBlank() || password.length < 6 || name.isBlank()) {
            return Result.failure(IllegalArgumentException("Please fill all fields. Password ≥ 6 chars."))
        }
        prefs.edit {
            putString(KEY_EMAIL, email)
            putString(KEY_NAME, name)
            putString(KEY_PWD_HASH, password.hashCode().toString())
        }
        return Result.success(User(email, name))
    }

    suspend fun signIn(email: String, password: String): Result<User> {
        delay(700)
        val savedEmail = prefs.getString(KEY_EMAIL, null)
        val savedPwd = prefs.getString(KEY_PWD_HASH, null)
        // Для демо: либо matches сохранённый, либо любой email/password ≥ 6 символов
        return if (savedEmail != null && savedEmail == email && savedPwd == password.hashCode().toString()) {
            Result.success(User(savedEmail, prefs.getString(KEY_NAME, "") ?: ""))
        } else if (email.contains("@") && password.length >= 6) {
            val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            prefs.edit {
                putString(KEY_EMAIL, email)
                putString(KEY_NAME, name)
                putString(KEY_PWD_HASH, password.hashCode().toString())
            }
            Result.success(User(email, name))
        } else {
            Result.failure(IllegalArgumentException("Invalid email or password"))
        }
    }

    suspend fun signInWithGoogle(): Result<User> {
        delay(600)
        // mock — в реальности тут GoogleSignInClient / Credential Manager
        val email = "demo.user@gmail.com"
        val name = "Demo User"
        prefs.edit {
            putString(KEY_EMAIL, email)
            putString(KEY_NAME, name)
            putString(KEY_PWD_HASH, "oauth-google")
        }
        return Result.success(User(email, name))
    }

    fun signOut() {
        prefs.edit { clear() }
    }

    // ---------- PIN ----------

    fun hasPin(): Boolean = prefs.contains(KEY_PIN_HASH)

    fun savePin(pin: String) {
        prefs.edit { putString(KEY_PIN_HASH, pin.hashCode().toString()) }
    }

    fun checkPin(pin: String): Boolean {
        val saved = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return saved == pin.hashCode().toString()
    }

    // ---------- Biometric ----------

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BIO, enabled) }
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIO, false)

    // ---------- Onboarding ----------

    fun hasSeenOnboarding(): Boolean = prefs.getBoolean(KEY_ONBOARDED, false)
    fun setOnboardingSeen() {
        prefs.edit { putBoolean(KEY_ONBOARDED, true) }
    }

    companion object {
        private const val PREFS = "equipay_auth"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "name"
        private const val KEY_PWD_HASH = "pwd"
        private const val KEY_PIN_HASH = "pin"
        private const val KEY_BIO = "bio"
        private const val KEY_ONBOARDED = "onboarded"

        @Volatile
        private var instance: AuthRepository? = null
        fun get(context: Context): AuthRepository =
            instance ?: synchronized(this) {
                instance ?: AuthRepository(context).also { instance = it }
            }
    }
}
