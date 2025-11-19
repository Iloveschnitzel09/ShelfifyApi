package com.shelfify.shelfifyapi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shelfify.shelfifyapi.service.UserService;

@RestController
public class RecipeController {

    @Autowired
    private UserService userService;

    @PostMapping("/createRecipie")
    public ResponseEntity<String> createRecipie(@RequestParam int id, @RequestParam String token) {
        try {
            System.out.println(id + " " + token);
            
            if (userService.checkToken(token, id)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ungültiger Token.");
            }

            // TODO: Implementiere Rezept-Erstellung
            return ResponseEntity.ok("Rezept wurde erfolgreich erstellt");
        } catch (Exception e) {
            System.out.println("Fehler beim Erstellen des Rezepts: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fehler beim Erstellen des Rezepts: " + e.getMessage());
        }
    }

}
