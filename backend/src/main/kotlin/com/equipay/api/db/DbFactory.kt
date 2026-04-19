package com.equipay.api.db

import com.equipay.api.config.DbConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.Transaction

object DbFactory {
    fun init(cfg: DbConfig): Database {
        val hikari = HikariConfig().apply {
            jdbcUrl = cfg.url
            username = cfg.user
            password = cfg.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        val ds = HikariDataSource(hikari)
        return Database.connect(ds)
    }
}

/** Short-hand for transaction block, suspending-friendly enough for hackathon scale. */
fun <T> dbQuery(block: Transaction.() -> T): T = transaction { block() }
