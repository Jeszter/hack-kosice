package com.hackkosice.server.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "wallet_members")
class WalletMemberEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    var wallet: WalletEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: WalletMemberRole = WalletMemberRole.OWNER,

    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null
) {
    @PrePersist
    fun prePersist() {
        if (createdAt == null) createdAt = Instant.now()
    }
}

enum class WalletMemberRole {
    OWNER,
    MEMBER
}
