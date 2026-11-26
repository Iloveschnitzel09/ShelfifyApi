package de.schnitzel.shelfifyapi.util

import jakarta.mail.internet.MimeMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Service
class EmailService (
    @Value("\${spring.mail.host}") private val host: String,
    @Value("\${spring.mail.port}") private val port: Int,
    @Value("\${spring.mail.username}") private val username: String,
    @Value("\${spring.mail.password}") private val password: String
) {
    @Async
    fun sendVerificationEmail(email: String, code: String) {

        val html = """
            <p>Hallo,</p>
            <p>vielen Dank für deine Registrierung bei Shelfify.</p>
            <p>Bitte bestätige deine E-Mail-Adresse, indem du den folgenden Verifizierungscode in der Shelfify-App eingibst:</p>
            <p style="font-size:18px; font-weight:italic; color:#6E0C19;">$code</p>
            <p>Viele Grüße,<br/>Dein Shelfify-Team</p>
        """.trimIndent()

        sendHtmlMail(email, "Verifizierung deines Kontos", html)
    }

    fun sendInvitationEmail(email: String, code: String) {
        val html = """
            <p>Hallo,</p>
        <p>du wurdest eingeladen, eine Gemeinsame Gruppe zu nutzen.</p>
        <p>Wenn sie beitreten wollen, geben sie folgenden Einladungscode in der Shelfify-App ein:</p>
        <p style="font-size:18px; font-style:italic; color:#6E0C19;">$code</p>
        <p>Wenn Sie diese Einladung nicht angesprochen haben, sollten Sie diese E-Mail einfach ignorieren.</p>
        <p>Wenn Sie keine Einladungen von dieser Datengruppe mehr erhalten möchten, können Sie sie in der Shelfify-App unter den Gruppen-Einstellungen blockieren. Geben Sie dazu den Code oben ein und tippen Sie auf <b>Gruppe blockieren</b>.</p>
        <p>Viele Grüße,<br/>Dein Shelfify-Team</p>
        """.trimIndent()

        sendHtmlMail(email, "Einladung zu Shelfify", html)
    }

    fun sendHtmlMail(to: String, subject: String, html: String) {
        try {
            val mailSender = getMailSender()
            val mimeMessage: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")

            helper.setFrom(username)
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(html, true) // true = HTML

            mailSender.send(mimeMessage)
        } catch (e: Exception) {
            throw RuntimeException("Fehler beim E-Mail-Versand: ${e.message}", e)
        }
    }

    fun getMailSender(): JavaMailSenderImpl {
        val mailSender = JavaMailSenderImpl()
        mailSender.host = host
        mailSender.port = port
        mailSender.username = username
        mailSender.password = password

        val props = mailSender.javaMailProperties
        props["mail.transport.protocol"] = "smtp"
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.starttls.required"] = "true"
        props["mail.smtp.connectiontimeout"] = "10000"
        props["mail.smtp.timeout"] = "10000"
        props["mail.smtp.writetimeout"] = "10000"
        props["mail.debug"] = "true"

        props["mail.smtp.ssl.checkserveridentity"] = "true"

        return mailSender
    }
}