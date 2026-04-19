package com.hackkosice.server.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "bank_connections")
class BankConnectionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    var wallet: WalletEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @Column(nullable = false)
    var bankName: String,

    @Column(nullable = false)
    var accountName: String,

    @Column(nullable = false)
    var iban: String,

    @Column(nullable = false, precision = 19, scale = 2)
    var balance: BigDecimal,

    @Column(nullable = false, length = 3)
    var currency: String,

    @Column(nullable = false)
    var status: String = "LINKED",

    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null
) {
    @PrePersist
    fun prePersist() {
        if (createdAt == null) createdAt = Instant.now()
    }
}
