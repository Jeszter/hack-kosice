package com.hackkosice.server.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:Size(min = 8, max = 100)
    val password: String,
    @field:NotBlank
    @field:Size(max = 120)
    val fullName: String
)

data class LoginRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: UserResponse
)

data class UserResponse(
    val id: Long,
    val email: String,
    val fullName: String,
    val role: String
)
