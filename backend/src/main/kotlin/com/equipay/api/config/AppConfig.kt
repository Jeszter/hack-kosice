package com.equipay.api.config

import io.ktor.server.application.ApplicationEnvironment

data class AppConfig(
    val db: DbConfig,
    val redis: RedisConfig,
    val email: EmailConfig,
    val jwt: JwtConfig,
    val openRouter: OpenRouterConfig,
    val tatraBank: TatraBankConfig
)

data class DbConfig(val url: String, val user: String, val password: String)
data class RedisConfig(val url: String)
data class EmailConfig(
    val provider: String,
    val resendApiKey: String,
    val smtpHost: String,
    val smtpPort: Int,
    val smtpUser: String,
    val smtpPassword: String,
    val from: String,
    val fromName: String
)
data class JwtConfig(
    val secret: String,
    val issuer: String,
    val accessTtlMin: Long,
    val refreshTtlDays: Long
)
data class OpenRouterConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String
)
data class TatraBankConfig(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val baseUrl: String,
    val sandboxMode: Boolean
)

fun loadConfig(env: ApplicationEnvironment): AppConfig {
    val c = env.config
    return AppConfig(
        db = DbConfig(
            url = c.property("database.url").getString(),
            user = c.property("database.user").getString(),
            password = c.property("database.password").getString()
        ),
        redis = RedisConfig(
            url = c.property("redis.url").getString()
        ),
        email = EmailConfig(
            provider = c.property("email.provider").getString(),
            resendApiKey = c.property("email.resendApiKey").getString(),
            smtpHost = c.property("email.smtpHost").getString(),
            smtpPort = c.property("email.smtpPort").getString().toInt(),
            smtpUser = c.property("email.smtpUser").getString(),
            smtpPassword = c.property("email.smtpPassword").getString(),
            from = c.property("email.from").getString(),
            fromName = c.property("email.fromName").getString()
        ),
        jwt = JwtConfig(
            secret = c.property("jwt.secret").getString(),
            issuer = c.property("jwt.issuer").getString(),
            accessTtlMin = c.property("jwt.accessTtlMin").getString().toLong(),
            refreshTtlDays = c.property("jwt.refreshTtlDays").getString().toLong()
        ),
        openRouter = OpenRouterConfig(
            apiKey = c.property("openRouter.apiKey").getString(),
            baseUrl = c.property("openRouter.baseUrl").getString(),
            model = c.property("openRouter.model").getString()
        ),
        tatraBank = TatraBankConfig(
            clientId = c.property("tatraBank.clientId").getString(),
            clientSecret = c.property("tatraBank.clientSecret").getString(),
            redirectUri = c.property("tatraBank.redirectUri").getString(),
            baseUrl = c.property("tatraBank.baseUrl").getString(),
            sandboxMode = c.property("tatraBank.sandboxMode").getString().toBoolean()
        )
    )
}