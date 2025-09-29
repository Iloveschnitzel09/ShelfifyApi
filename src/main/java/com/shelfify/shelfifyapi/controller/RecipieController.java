package com.shelfify.shelfifyapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecipieController {

    @PostMapping("/createRecipie")
    public ResponseEntity<String> createRecipie(@RequestParam int id, @RequestParam String token) {
        System.out.println(id + " " + token);
        return ResponseEntity.ok("Recipie created");
    }

}
