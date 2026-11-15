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
        props["mail.debug"] = "true"

        // TrustManager, der allen Zertifikaten vertraut
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                override fun checkClientTrusted(certs: Array<X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(certs: Array<X509Certificate>?, authType: String?) {}
            }
        )

        // all-trusting TrustManager installieren
        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, SecureRandom())

        props["mail.smtp.ssl.trust"] = "*"
        props["mail.smtp.ssl.socketFactory"] = sc.socketFactory
        props["mail.smtp.ssl.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"

        return mailSender
    }
}