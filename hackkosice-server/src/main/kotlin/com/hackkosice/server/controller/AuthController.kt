package com.hackkosice.server.controller

import com.hackkosice.server.dto.AuthResponse
import com.hackkosice.server.dto.LoginRequest
import com.hackkosice.server.dto.RegisterRequest
import com.hackkosice.server.dto.UserResponse
import com.hackkosice.server.entity.UserEntity
import com.hackkosice.server.service.AuthService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse {
        return authService.register(request)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse {
        return authService.login(request)
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal currentUser: UserEntity): UserResponse {
        return authService.me(currentUser)
    }
}
