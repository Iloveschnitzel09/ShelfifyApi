package com.shelfify.shelfifyapi.controller;

import com.shelfify.shelfifyapi.repository.NotificationSettingsRepository;
import com.shelfify.shelfifyapi.repository.UserRepository;
import com.shelfify.shelfifyapi.scheduler.ExpirationCheckScheduler;
import com.shelfify.shelfifyapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class UserAccController {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private NotificationSettingsRepository notificationRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private UserService userService;

    @GetMapping("/appSync")
    public ResponseEntity<Map<String, Object>> appSync(@RequestParam(required = false) Integer id, @RequestParam(required = true) String token) {
        if (id == null) {
            // Neue ID erzeugen und zurückgeben
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO users (email, notify, datagroup, verified, token, own_datagroup) VALUES (NULL, FALSE, ?, FALSE, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                String datagroup = UUID.randomUUID().toString();
                ps.setString(1, datagroup);
                ps.setString(2, token);
                ps.setString(3, datagroup);
                return ps;
            }, keyHolder);

            Number newId = keyHolder.getKey();
            if (newId == null) {
                return ResponseEntity.ok(Map.of("error", "ID konnte nicht erzeugt werden"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", newId.intValue());
            response.put("email", null);
            response.put("notify", false);
            response.put("verified", false);
            return ResponseEntity.ok(response);
        } else {
            try {
                if (token.equals(notificationRepo.getToken(id))) {
                    Map<String, Object> user = jdbc.queryForMap("SELECT * FROM users WHERE id = ?", id);
                    return ResponseEntity.ok(user);
                }
                return ResponseEntity.status(404).body(Map.of("error", "Token Falsch"));
            } catch (EmptyResultDataAccessException e) {
                return ResponseEntity.status(404).body(Map.of("error", "ID nicht gefunden"));
            }
        }
    }

    @PostMapping("/setEmail")
    public ResponseEntity<String> setEmail(@RequestParam String email, @RequestParam int id, @RequestParam String token) {
        try {
            System.out.println(id + " " + token);
            if(userService.checkToken(token, id)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            if (notificationRepo.checkEmail(email)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            notificationRepo.saveEmail(email, id);
            notificationRepo.setCode(null, id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/deleteAcc")
    public ResponseEntity<String> deleteAcc(@RequestParam int id, @RequestParam String token) {
        try {
            if(userService.checkToken(token, id)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            userService.deleteUserData(id);

            return ResponseEntity.ok("Benutzerkonto und alle zugehörigen Daten wurden gelöscht.");
        } catch (Exception e) {
            System.out.println("2" + e);
            return ResponseEntity.badRequest().body("Fehler beim Löschen des Kontos: " + e.getMessage());
        }
    }

    /// TEST KRAM


    @Autowired
    private ExpirationCheckScheduler emailService;

    @GetMapping("/sendTestEmail")
    public ResponseEntity<String> sendTestEmail() {
        try {
            String[] email = notificationRepo.getAllNotificationEmails().toArray(new String[0]);
            if (email[0] == null) {
                return ResponseEntity.badRequest().body("Keine E-Mail-Adresse gespeichert!");
            }

            emailService.checkExpiringProducts();

            return ResponseEntity.ok("Test-E-Mail wurde gesendet an: " + Arrays.toString(email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Fehler beim Senden: " + e.getMessage());
        }
    }
}
