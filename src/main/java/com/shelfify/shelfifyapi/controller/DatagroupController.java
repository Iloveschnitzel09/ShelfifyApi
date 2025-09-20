package com.shelfify.shelfifyapi.controller;

import com.shelfify.shelfifyapi.repository.NotificationSettingsRepository;
import com.shelfify.shelfifyapi.repository.UserRepository;
import com.shelfify.shelfifyapi.service.DatagroupService;
import com.shelfify.shelfifyapi.service.EmailService;
import com.shelfify.shelfifyapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @Autowired
    DatagroupService datagroupService;

    @PostMapping("/inviteToDatagroup")
    public ResponseEntity<String> inviteToDatagroup(@RequestParam int id, @RequestParam String token, @RequestParam String email) {
        System.out.println("inviteToDatagroup" + id + " " + token + " " + email);
        if (userService.checkToken(token, id)) {
            return ResponseEntity.status(300).build(); // Token ungültig
        } else if (notificationRepo.checkBlocked(email, id)) {
            return ResponseEntity.status(303).build(); // E-Mail blockiert
        } else if (!notificationRepo.checkEmail(email)) {
            return ResponseEntity.status(330).build(); // E-Mail schon vorhanden
        }
        String invCode = datagroupService.createInvitationCode(notificationRepo.getDatagroup(id));
        emailService.sendSimpleEmail(email, "Einladung zur Datengruppe", invCode);
        return ResponseEntity.ok("Invitation sent");
    }

    @PostMapping("/joinDatagroup")
    public ResponseEntity<String> joinDatagroup(@RequestParam int id, @RequestParam String token, @RequestParam String code) {
        System.out.println("joinDatagroup" + id + " " + token + " " + code);
        String email = userService.getEmail(id);
        if(userService.checkToken(token, id) || notificationRepo.checkBlocked(email, id) || !notificationRepo.checkEmail(email)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String datagroup = datagroupService.getDatagroupByCode(code);
        if (datagroup == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userService.setDatagroup(id, datagroup);
        return ResponseEntity.ok("User joined datagroup");
    }
}
