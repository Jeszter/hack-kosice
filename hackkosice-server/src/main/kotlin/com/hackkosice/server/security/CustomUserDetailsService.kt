package com.hackkosice.server.security

import com.hackkosice.server.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(username)
            .orElseThrow { UsernameNotFoundException("User not found") }

        return User.builder()
            .username(user.email)
            .password(user.passwordHash)
            .authorities(listOf(SimpleGrantedAuthority("ROLE_${user.role.name}")))
            .build()
    }
}
