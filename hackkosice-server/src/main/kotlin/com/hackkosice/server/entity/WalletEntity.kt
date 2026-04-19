package com.hackkosice.server.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "wallets")
class WalletEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, length = 3)
    var currency: String = "EUR",

    @Column(nullable = false, precision = 19, scale = 2)
    var totalBalance: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null,

    @OneToMany(mappedBy = "wallet")
    var members: MutableList<WalletMemberEntity> = mutableListOf(),

    @OneToMany(mappedBy = "wallet")
    var bankConnections: MutableList<BankConnectionEntity> = mutableListOf()
) {
    @PrePersist
    fun prePersist() {
        if (createdAt == null) createdAt = Instant.now()
    }
}
