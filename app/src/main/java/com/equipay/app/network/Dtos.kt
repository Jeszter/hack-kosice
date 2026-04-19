package com.equipay.app.network

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String? = null
)

@Serializable
data class RegisterResponse(
    val userId: String,
    val email: String,
    val requiresEmailVerification: Boolean = true
)

@Serializable
data class VerifyEmailRequest(
    val email: String,
    val code: String,
    val deviceId: String? = null,
    val deviceName: String? = null
)

@Serializable
data class ResendCodeRequest(val email: String)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String? = null,
    val deviceName: String? = null
)

@Serializable
data class LoginWithPinRequest(
    val email: String,
    val pin: String,
    val deviceId: String? = null,
    val deviceName: String? = null
)

@Serializable
data class SetPinRequest(val pin: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: String,
    val userId: String,
    val email: String,
    val hasPin: Boolean
)

@Serializable
data class OkResponse(val ok: Boolean = true, val message: String? = null)

@Serializable
data class ErrorResponse(val error: String, val code: String? = null)
