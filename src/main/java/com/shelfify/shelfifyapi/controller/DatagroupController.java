package com.shelfify.shelfifyapi.controller;

import com.shelfify.shelfifyapi.repository.NotificationSettingsRepository;
import com.shelfify.shelfifyapi.repository.UserRepository;
import com.shelfify.shelfifyapi.service.DatagroupService;
import com.shelfify.shelfifyapi.service.EmailService;
import com.shelfify.shelfifyapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class DatagroupController {

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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } else if (notificationRepo.checkBlocked(email, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } else if (!notificationRepo.checkEmail(email)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
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

    @PostMapping("/leaveDatagroup")
    public ResponseEntity<String> leaveDatagroup(@RequestParam int id, @RequestParam String token) {
        if (userService.checkToken(token, id)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        userService.leaveDatagroup(id);
        return ResponseEntity.ok("User left datagroup");
    }

    @GetMapping("/datagroupMembers")
    public List<Map<String, List<String>>> datagroupMembers(@RequestParam int id, @RequestParam String token) {
        if (userService.checkToken(token, id)) return null;

        String datagroup = userService.getDatagroup(id);
        String ownDatagroup = userService.getOwnDatagroup(id);

        List<Map<String, List<String>>> reply = new ArrayList<>(List.of());

        List<String> own = Collections.singletonList(datagroup.equals(ownDatagroup) ? "true" : "false");
        reply.add(Map.of("owner", own));


        List<String> mails = datagroupService.getEmailsFromDatagroup(datagroup);
        reply.add(Map.of("members", mails));

        return reply;
    }

    @PostMapping("/kickFromDatagroup")
    public ResponseEntity<String> kickFromDatagroup(@RequestParam int id, @RequestParam String token, @RequestParam String email) {
        if (userService.checkToken(token, id)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String datagroup = userService.getDatagroup(id);
        String ownDatagroup = userService.getOwnDatagroup(id);

        if (!datagroup.equals(ownDatagroup)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (!notificationRepo.checkEmail(email)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        int kickId = userService.getId(email);
        userService.leaveDatagroup(kickId);
        return ResponseEntity.ok().build();
    }
}
