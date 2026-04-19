package com.hackkosice.server.service

import com.hackkosice.server.dto.AuthResponse
import com.hackkosice.server.dto.LoginRequest
import com.hackkosice.server.dto.RegisterRequest
import com.hackkosice.server.dto.UserResponse
import com.hackkosice.server.entity.UserEntity
import com.hackkosice.server.repository.UserRepository
import com.hackkosice.server.security.JwtService
import jakarta.transaction.Transactional
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager
) {
    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val normalizedEmail = request.email.trim().lowercase()
        if (userRepository.existsByEmail(normalizedEmail)) {
            error("User with this email already exists")
        }

        val user = userRepository.save(
            UserEntity(
                email = normalizedEmail,
                passwordHash = passwordEncoder.encode(request.password),
                fullName = request.fullName.trim()
            )
        )

        return AuthResponse(
            token = jwtService.generateToken(user),
            user = user.toResponse()
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        val normalizedEmail = request.email.trim().lowercase()

        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(normalizedEmail, request.password)
        )

        val user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow { IllegalArgumentException("Invalid credentials") }

        return AuthResponse(
            token = jwtService.generateToken(user),
            user = user.toResponse()
        )
    }

    fun me(currentUser: UserEntity): UserResponse {
        return currentUser.toResponse()
    }

    private fun UserEntity.toResponse(): UserResponse {
        return UserResponse(
            id = id!!,
            email = email,
            fullName = fullName,
            role = role.name
        )
    }
}
