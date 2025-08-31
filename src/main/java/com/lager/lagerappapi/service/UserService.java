package com.lager.lagerappapi.service;

import com.lager.lagerappapi.model.User;
import com.lager.lagerappapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    public void sendVerificationCode(String email) {
        // Generiere einen 6-stelligen Code
        String verificationCode = generateVerificationCode();

        // Speichere oder aktualisiere den Benutzer
        User user = userRepository.findByEmail(email)
                .orElse(new User());
        user.setEmail(email);
        user.setVerificationCode(verificationCode);
        user.setVerified(false);
        userRepository.save(user);

        // Sende den Code per E-Mail (direkt, ohne Verifizierungsprüfung)
        sendVerificationEmail(email, verificationCode);
    }

    private void sendVerificationEmail(String email, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Ihr Verifizierungscode für die LagerApp");
            message.setText(String.format(
                "Ihr Verifizierungscode lautet: %s\n\n" +
                "Bitte geben Sie diesen Code in der App ein, um Ihre E-Mail-Adresse zu verifizieren.",
                code
            ));
            emailService.getMailSender().send(message);
        } catch (Exception e) {
            System.out.println("Fehler beim Senden des Verifizierungscodes: " + e.getMessage());
        }
    }

    public boolean verifyCode(String email, String code) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if (user.getVerificationCode().equals(code)) {
                        user.setVerified(true);
                        userRepository.save(user);
                        return true;

                    }
                    return false;
                })
                .orElse(false);
    }

    public boolean isEmailVerified(String email) {
        return userRepository.findByEmail(email)
                .map(User::isVerified)
                .orElse(false);
    }

    public void setNotifyPreference(String email, boolean notify) {
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    user.setNotify(notify);
                    userRepository.save(user);
                });
    }

    public boolean checkToken(String token, String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        return userOpt.isEmpty() || !userOpt.get().getToken().equals(token);
    }

    public boolean checkToken(String token, int id) {
        Optional<User> userOpt = userRepository.findById(id);
        return userOpt.isEmpty() || !userOpt.get().getToken().equals(token);
    }

    public boolean checkInvite(String email, int id) {
        userRepository.findById(id)
                .ifPresent(user -> {

                });
        return false;
    }

    public void deleteUserData(int id) {
        userRepository.deleteById(id);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
} 