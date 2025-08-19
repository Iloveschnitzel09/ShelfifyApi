package com.lager.lagerappapi.controller;

import com.lager.lagerappapi.repository.UserRepository;
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

    public ResponseEntity<String> inviteToDatagroup(@RequestParam String email, @RequestParam int id, @RequestParam String token) {
        if (userService.checkToken(token, id)) return ResponseEntity.status(401).build();

        return ResponseEntity.ok("User invited to datagroup");
    }
}
