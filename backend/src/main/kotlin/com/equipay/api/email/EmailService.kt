package com.equipay.api.email

import com.equipay.api.config.EmailConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import org.slf4j.LoggerFactory

interface EmailService {
    fun sendVerificationCode(to: String, code: String)
    fun sendInvitation(to: String, inviterName: String, groupName: String, inviteLink: String)
}

class EmailServiceFactory {
    companion object {
        fun create(cfg: EmailConfig): EmailService =
            when (cfg.provider.lowercase()) {
                "resend" -> ResendEmailService(cfg)
                else -> SmtpEmailService(cfg)
            }
    }
}

private val log = LoggerFactory.getLogger("EmailService")

class ResendEmailService(private val cfg: EmailConfig) : EmailService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
    }

    @Serializable
    private data class ResendRequest(
        val from: String,
        val to: List<String>,
        val subject: String,
        val html: String
    )

    override fun sendVerificationCode(to: String, code: String) {
        val html = """
            <div style="font-family:-apple-system,BlinkMacSystemFont,sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#0f0f0f;color:#fff;border-radius:16px;">
              <h1 style="font-size:28px;margin:0 0 16px;">EquiPay verification</h1>
              <p style="color:#b3b3b3;margin:0 0 24px;font-size:14px;">
                Use this code to verify your email:
              </p>
              <div style="font-size:40px;font-weight:bold;letter-spacing:8px;padding:24px;background:#1c1c1e;border-radius:12px;text-align:center;">$code</div>
              <p style="color:#8a8a8e;margin-top:24px;font-size:12px;">
                Code expires in 10 minutes. If you didn't request it, ignore this email.
              </p>
            </div>
        """.trimIndent()
        send(to, "Your EquiPay verification code: $code", html)
    }

    override fun sendInvitation(to: String, inviterName: String, groupName: String, inviteLink: String) {
        val html = """
            <div style="font-family:-apple-system,BlinkMacSystemFont,sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#0f0f0f;color:#fff;border-radius:16px;">
              <h1 style="font-size:24px;margin:0 0 16px;">You're invited!</h1>
              <p style="color:#b3b3b3;font-size:14px;">
                <strong>$inviterName</strong> invited you to join the group <strong>$groupName</strong> on EquiPay.
              </p>
              <a href="$inviteLink" style="display:inline-block;padding:14px 24px;background:#fff;color:#000;border-radius:10px;text-decoration:none;font-weight:600;margin-top:16px;">Accept invitation</a>
            </div>
        """.trimIndent()
        send(to, "$inviterName invited you to $groupName on EquiPay", html)
    }

    private fun send(to: String, subject: String, html: String) {
        if (cfg.resendApiKey.isBlank()) {
            log.warn("Resend API key is empty — skipping email to $to. Subject: $subject")
            return
        }
        runBlocking {
            try {
                val resp = client.post("https://api.resend.com/emails") {
                    header("Authorization", "Bearer ${cfg.resendApiKey}")
                    header("Content-Type", "application/json")
                    setBody(
                        ResendRequest(
                            from = "${cfg.fromName} <${cfg.from}>",
                            to = listOf(to),
                            subject = subject,
                            html = html
                        )
                    )
                }
                if (!resp.status.isSuccess()) {
                    log.error("Resend error ${resp.status}: ${resp.bodyAsText()}")
                }
            } catch (e: Exception) {
                log.error("Failed to send email via Resend", e)
            }
        }
    }
}

class SmtpEmailService(private val cfg: EmailConfig) : EmailService {
    private val mailer = MailerBuilder
        .withSMTPServer(cfg.smtpHost, cfg.smtpPort, cfg.smtpUser, cfg.smtpPassword)
        .withTransportStrategy(TransportStrategy.SMTP_TLS)
        .buildMailer()

    override fun sendVerificationCode(to: String, code: String) {
        val email = EmailBuilder.startingBlank()
            .from(cfg.fromName, cfg.from)
            .to(to)
            .withSubject("Your EquiPay verification code")
            .withPlainText(
                """
                Welcome to EquiPay!

                Your verification code is: $code

                This code expires in 10 minutes.
                """.trimIndent()
            )
            .buildEmail()
        mailer.sendMail(email)
    }

    override fun sendInvitation(to: String, inviterName: String, groupName: String, inviteLink: String) {
        val email = EmailBuilder.startingBlank()
            .from(cfg.fromName, cfg.from)
            .to(to)
            .withSubject("$inviterName invited you to $groupName on EquiPay")
            .withPlainText(
                """
                $inviterName invited you to join "$groupName" on EquiPay.

                Accept the invitation: $inviteLink
                """.trimIndent()
            )
            .buildEmail()
        mailer.sendMail(email)
    }
}