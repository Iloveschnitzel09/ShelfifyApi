package com.lager.lagerappapi.controller;

import com.lager.lagerappapi.repository.NotificationSettingsRepository;
import com.lager.lagerappapi.repository.UserRepository;
import com.lager.lagerappapi.service.EmailService;
import com.lager.lagerappapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    public ResponseEntity<String> inviteToDatagroup(@RequestParam String email, @RequestParam int id, @RequestParam String token) {
        if (userService.checkToken(token, id) || notificationRepo.checkBlocked(email, id) || notificationRepo.checkEmail(email)) return ResponseEntity.status(401).build();
        emailService.sendSimpleEmail(email, "Einladung zur Datengruppe",
                "Du wurdest eingeladen, der Datengruppe beizutreten. Klicke auf den Link, um beizutreten: https://lagerapp.de/invite?datagroup="
                + notificationRepo.getDatagroup(id));
        return ResponseEntity.ok("User invited to datagroup");
    }

    public ResponseEntity<String> joinDatagroup(@RequestParam int id, @RequestParam String token, @RequestParam String datagroup) {

        return ResponseEntity.ok("User joined datagroup");
    }
}
