package com.lager.lagerappapi.controller;

import com.lager.lagerappapi.repository.NotificationSettingsRepository;
import com.lager.lagerappapi.repository.UserRepository;
import com.lager.lagerappapi.service.EmailService;
import com.lager.lagerappapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DatagroupController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserService userService;

    @Autowired
    NotificationSettingsRepository notificationRepo;

    @Autowired
    EmailService  emailService;


    @PostMapping("/inviteToDatagroup")
    public ResponseEntity<String> inviteToDatagroup(@RequestParam String email, @RequestParam int id, @RequestParam String token) {
        if (userService.checkToken(token, id) || notificationRepo.checkBlocked(email, id) || notificationRepo.checkEmail(email)) return ResponseEntity.status(401).build();
        emailService.sendSimpleEmail(email, "Einladung zur Datengruppe",
                "Du wurdest eingeladen, der Datengruppe beizutreten. Klicke auf den Link, um beizutreten: <a href=\"shelfify://invite?datagroup=" +
                        notificationRepo.getDatagroup(id) +
                        "\">Jetzt in der App öffnen</a>\n");
        return ResponseEntity.ok("Invitation sent");
    }

    @PostMapping("/joinDatagroup")
    public ResponseEntity<String> joinDatagroup(@RequestParam String email, @RequestParam String token, @RequestParam String datagroup) {
        int id = userService.getId(email);
        if (userService.checkToken(token, email) || notificationRepo.checkBlocked(email, id)) return ResponseEntity.status(401).build();
        userService.setDatagroup(id, datagroup);
        return ResponseEntity.ok("User joined datagroup");
    }
}
