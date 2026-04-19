package com.hackkosice.server.repository

import com.hackkosice.server.entity.WalletEntity
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface WalletRepository : JpaRepository<WalletEntity, Long> {
    @EntityGraph(attributePaths = ["members", "members.user", "bankConnections", "bankConnections.user"])
    override fun findById(id: Long): Optional<WalletEntity>
}
