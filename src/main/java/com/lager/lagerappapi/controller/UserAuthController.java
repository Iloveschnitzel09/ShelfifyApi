package com.lager.lagerappapi.controller;

import com.lager.lagerappapi.repository.NotificationSettingsRepository;
import com.lager.lagerappapi.repository.UserRepository;
import com.lager.lagerappapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserAuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationSettingsRepository notificationRepo;

    @PostMapping("/requestVerificode")
    public ResponseEntity<String> requestVerificode(@RequestParam String email, @RequestParam String token) {
        try {
            System.out.println(email + " " + token);
            if(userService.checkToken(token, email)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            userService.sendVerificationCode(email);
            return ResponseEntity.ok("Verifizierungscode wurde gesendet");

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.badRequest().body("Fehler beim Senden des Codes: " + e.getMessage());
        }
    }

    @PostMapping("/verifyCode")
    public ResponseEntity<String> verifyCode(@RequestParam String email, @RequestParam String code, @RequestParam String token) {
        try {
            if(userService.checkToken(token, email)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            boolean isValid = userService.verifyCode(email, code);
            if (isValid) {
                return ResponseEntity.ok("E-Mail erfolgreich verifiziert");
            } else {
                return ResponseEntity.badRequest().body("Ungültiger Code");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Fehler bei der Verifizierung: " + e.getMessage());
        }
    }

    @PostMapping("/setNotifyPreference")
    public ResponseEntity<String> setNotifyPreference(@RequestParam String email, @RequestParam String token) {
        try {
            if(userService.checkToken(token, email)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            if (!userService.isEmailVerified(email)) {
                return ResponseEntity.badRequest().body("E-Mail muss zuerst verifiziert werden");
            }

            boolean notify = !userRepository.findByEmail(email).get().isNotify();

            userService.setNotifyPreference(email, notify);
            return ResponseEntity.ok("Benachrichtigungseinstellung aktualisiert");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Fehler beim Aktualisieren der Einstellung: " + e.getMessage());
        }
    }
}
