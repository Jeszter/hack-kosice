package com.hackkosice.server.security

import com.hackkosice.server.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.removePrefix("Bearer ").trim()

        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response)
            return
        }

        val userId = jwtService.extractUserId(token)
        val user = userRepository.findById(userId).orElse(null)

        if (user != null && SecurityContextHolder.getContext().authentication == null) {
            val auth = UsernamePasswordAuthenticationToken(
                user,
                null,
                listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
            )
            SecurityContextHolder.getContext().authentication = auth
        }

        filterChain.doFilter(request, response)
    }
}
