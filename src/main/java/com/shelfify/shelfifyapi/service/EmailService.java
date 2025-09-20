package com.shelfify.shelfifyapi.service;

import com.shelfify.shelfifyapi.model.Products;
import com.shelfify.shelfifyapi.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Properties;

@Service
public class EmailService {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Autowired
    private UserRepository userRepository;

    public JavaMailSenderImpl getMailSender() throws Exception {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        // Erstelle einen TrustManager, der allen Zertifikaten vertraut
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                public void checkServerTrusted(X509Certificate[] certs, String authType) { }
            }
        };

        // Installiere den all-trusting TrustManager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());

        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.smtp.ssl.socketFactory", sc.getSocketFactory());
        props.put("mail.smtp.ssl.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        return mailSender;
    }

    public void sendSimpleEmail(String to, String subject, String invCode) {
        // Überprüfe, ob der Benutzer verifiziert ist und Benachrichtigungen aktiviert hat
        userRepository.findByEmail(to)
//                .filter(user -> user.isVerified() && user.isNotify())
                .ifPresent(user -> {
                    try {
                        MimeMessage mimeMessage = getMailSender().createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                        helper.setTo(to);
                        helper.setSubject(subject);

                        String html = "<p>Hallo,</p>"
                                + "<p>du wurdest eingeladen, einer Datengruppe in Shelfify beizutreten.</p>"
                                + "<p>Bitte öffne die Shelfify-App und gib folgenden Einladungscode innerhalb von 5 Minuten ein:</p>"
                                + "<p style=\"font-size:18px; font-weight:bold; color:#2c3e50;\">" + invCode + "</p>"
                                + "<p>So funktioniert’s:</p>"
                                + "<ol>"
                                + "  <li>Shelfify-App öffnen</li>"
                                + "  <li>Zum Bereich <b>Datengruppe beitreten</b> gehen</li>"
                                + "  <li>Den obigen Einladungscode einfügen</li>"
                                + "</ol>"
                                + "<p>Viele Grüße,<br/>Dein Shelfify-Team</p>";


                        helper.setText(html, true); // true = HTML
                        getMailSender().send(mimeMessage);

                    } catch (Exception e) {
                        throw new RuntimeException("Fehler beim E-Mail-Versand: " + e.getMessage());
                    }
                });
    }

    public void sendExpirationAlert(List<Products> expiredProducts, List<Products> expiringProducts) {
        // Erstelle den E-Mail-Text
        StringBuilder body = new StringBuilder();

        // Bereits abgelaufene Produkte
        if (!expiredProducts.isEmpty()) {
            body.append("ABGELAUFENE PRODUKTE:\n");
            body.append("====================\n\n");

            for (Products product : expiredProducts) {
                body.append(String.format("- %s (Menge: %d) ist am %s abgelaufen\n",
                    product.getProduktname(),
                    product.getMenge(),
                    product.getAblaufdatum().toString()));
            }
            body.append("\n");
        }

        // Bald ablaufende Produkte
        if (!expiringProducts.isEmpty()) {
            body.append("BALD ABLAUFENDE PRODUKTE:\n");
            body.append("========================\n\n");

            for (Products product : expiringProducts) {
                body.append(String.format("- %s (Menge: %d) läuft am %s ab\n",
                    product.getProduktname(),
                    product.getMenge(),
                    product.getAblaufdatum().toString()));
            }
        }

        String messageText = body.toString();

        // Sende nur an verifizierte Benutzer mit aktivierten Benachrichtigungen
        userRepository.findAll().stream()
            .filter(user -> user.isVerified() && user.isNotify())
            .forEach(user -> {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(user.getEmail());
                    message.setSubject("Produktablauf-Benachrichtigung");
                    message.setText(messageText);
                    getMailSender().send(message);
                } catch (Exception e) {
                    // Log error but continue with other users
                    System.err.println("Fehler beim Senden an " + user.getEmail() + ": " + e.getMessage());
                }
            });
    }
} 