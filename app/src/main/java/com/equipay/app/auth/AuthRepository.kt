package com.equipay.app.auth

import com.equipay.app.network.ApiClient
import com.equipay.app.network.ErrorResponse
import com.equipay.app.network.LoginRequest
import com.equipay.app.network.LoginWithPinRequest
import com.equipay.app.network.OkResponse
import com.equipay.app.network.RefreshRequest
import com.equipay.app.network.RegisterRequest
import com.equipay.app.network.RegisterResponse
import com.equipay.app.network.ResendCodeRequest
import com.equipay.app.network.SetPinRequest
import com.equipay.app.network.TokenResponse
import com.equipay.app.network.VerifyEmailRequest
import kotlinx.serialization.json.Json
import retrofit2.Response

sealed class AuthError(message: String) : Exception(message) {
    class Network(message: String) : AuthError(message)
    class Server(message: String) : AuthError(message)
    class InvalidInput(message: String) : AuthError(message)
}

class AuthRepository(private val tokenStore: TokenStore) {
    private val api get() = ApiClient.authApi

    suspend fun register(email: String, password: String, displayName: String?): Result<RegisterResponse> =
        call { api.register(RegisterRequest(email, password, displayName)) }

    suspend fun verifyEmail(email: String, code: String, deviceId: String?): Result<TokenResponse> =
        call { api.verifyEmail(VerifyEmailRequest(email, code, deviceId, android.os.Build.MODEL)) }
            .onSuccess { saveAndActivate(it) }

    suspend fun resendCode(email: String): Result<OkResponse> =
        call { api.resendCode(ResendCodeRequest(email)) }

    suspend fun loginWithPassword(email: String, password: String, deviceId: String?): Result<TokenResponse> =
        call { api.login(LoginRequest(email, password, deviceId, android.os.Build.MODEL)) }
            .onSuccess { saveAndActivate(it) }

    suspend fun loginWithPin(email: String, pin: String, deviceId: String?): Result<TokenResponse> =
        call { api.loginWithPin(LoginWithPinRequest(email, pin, deviceId, android.os.Build.MODEL)) }
            .onSuccess { saveAndActivate(it) }

    suspend fun setPin(pin: String): Result<OkResponse> {
        val access = AccessTokenHolder.get()
            ?: return Result.failure(AuthError.Server("Not signed in"))
        return call { api.setPin("Bearer $access", SetPinRequest(pin)) }
            .onSuccess { tokenStore.setHasPin(true) }
    }

    suspend fun logout(): Result<OkResponse> {
        val refresh = tokenStore.getRefresh()
        val res = if (refresh != null) call { api.logout(RefreshRequest(refresh)) } else Result.success(OkResponse())
        tokenStore.clear()
        AccessTokenHolder.clear()
        return res
    }

    /** Попытаться восстановить сессию через refresh. */
    suspend fun tryRestoreSession(): Boolean {
        val refresh = tokenStore.getRefresh() ?: return false
        val res = call { api.refresh(RefreshRequest(refresh)) }
        return res.fold(
            onSuccess = { saveAndActivate(it); true },
            onFailure = { tokenStore.clear(); AccessTokenHolder.clear(); false }
        )
    }

    private fun saveAndActivate(t: TokenResponse) {
        tokenStore.saveSession(
            refreshToken = t.refreshToken,
            email = t.email,
            userId = t.userId,
            hasPin = t.hasPin
        )
        AccessTokenHolder.set(t.accessToken)
    }

    private val errorJson = Json { ignoreUnknownKeys = true }

    private suspend fun <T> call(block: suspend () -> Response<T>): Result<T> = try {
        val resp = block()
        if (resp.isSuccessful) {
            val body = resp.body()
            if (body != null) Result.success(body)
            else Result.failure(AuthError.Server("Empty body"))
        } else {
            val raw = resp.errorBody()?.string().orEmpty()
            val message = try {
                errorJson.decodeFromString(ErrorResponse.serializer(), raw).error
            } catch (_: Exception) {
                raw.ifBlank { "Error ${resp.code()}" }
            }
            Result.failure(AuthError.Server(message))
        }
    } catch (e: Exception) {
        Result.failure(AuthError.Network(e.message ?: "Network error"))
    }
}
