package com.shelfify.shelfifyapi.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.shelfify.shelfifyapi.ean.EanMapping;
import com.shelfify.shelfifyapi.ean.EanMappingRepository;
import com.shelfify.shelfifyapi.model.Products;
import com.shelfify.shelfifyapi.repository.ProduktRepository;
import com.shelfify.shelfifyapi.repository.UserRepository;
import com.shelfify.shelfifyapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

@RestController
public class ProductController {

    @Autowired
    private ProduktRepository produktRepository;

    @Autowired
    private EanMappingRepository eanMappingRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private DataSource dataSource;

    @GetMapping("/products")
    public List<Products> getAllProducts(@RequestParam int id, @RequestParam String token) {
        if (userService.checkToken(token, id)) return null;
        return produktRepository.findProductsByDatagroup(
                userService.getDatagroup(id),
                Sort.by(Sort.Order.asc("produktname"), Sort.Order.asc("ablaufdatum"))
        );
    }


    @GetMapping("/spoiledProducts")
    public List<Products> getSpoiledProducts(@RequestParam(defaultValue = "10") int days, @RequestParam int id, @RequestParam String token) {
        if (userService.checkToken(token, id)) return null;

        LocalDate cutoffDate = LocalDate.now().plusDays(days);
        return produktRepository.findByAblaufdatumBeforeAndDatagroup(
                cutoffDate,
                userService.getDatagroup(id));
    }

    // TODO: Need to be converted to work with datagroups
    @GetMapping("/lookupProductName")
    public ResponseEntity<String> lookupProductName(@RequestParam String ean) {
        try {
            return eanMappingRepository.findByEanAndDatagroup(ean, "global")
                    .map(mapping -> ResponseEntity.ok(mapping.getProductName()))
                    .orElseGet(() -> fetchAndStoreProductNameFromApi(ean));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fehler bei der Produktabfrage.");
        }
    }

    private ResponseEntity<String> fetchAndStoreProductNameFromApi(String ean) {
        try {
            String apiUrl = "https://world.openfoodfacts.org/api/v2/product/" + ean + ".json";
            String jsonResponse = new Scanner(new URL(apiUrl).openStream(), StandardCharsets.UTF_8).useDelimiter("\\A").next();
            String productName = new ObjectMapper().readTree(jsonResponse)
                    .path("product").path("product_name").asText();

            if (productName != null && !productName.isEmpty()) {
                EanMapping newEntry = new EanMapping();
                newEntry.setEan(ean);
                newEntry.setProductName(productName);
                eanMappingRepository.save(newEntry);
                return ResponseEntity.ok(productName);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Produktname nicht gefunden.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fehler beim Zugriff auf OpenFoodFacts.");
        }
    }

    @PostMapping("/addProduct")
    public ResponseEntity<String> addProduct(@RequestParam String name, @RequestParam String ablaufdatum, @RequestParam int id, @RequestParam String token) {
        if (userService.checkToken(token, id)) return null;
        String datagroup = userService.getDatagroup(id);

        try (Connection connection = dataSource.getConnection()) {
            java.sql.Date sqlDate = java.sql.Date.valueOf(ablaufdatum);

            String checkQuery = "SELECT menge FROM lebensmittel WHERE produktname = ? AND ablaufdatum = ? AND datagroup = ?";
            PreparedStatement pstmt = connection.prepareStatement(checkQuery);
            pstmt.setString(1, name);
            pstmt.setDate(2, sqlDate);
            pstmt.setString(3, datagroup);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int currentMenge = rs.getInt("menge");
                PreparedStatement updateStmt = connection.prepareStatement(
                        "UPDATE lebensmittel SET menge = ? WHERE produktname = ? AND ablaufdatum = ? AND datagroup = ?");
                updateStmt.setInt(1, currentMenge + 1);
                updateStmt.setString(2, name);
                updateStmt.setDate(3, sqlDate);
                updateStmt.setString(4, datagroup);
                updateStmt.executeUpdate();
                return ResponseEntity.ok("Produktmenge für " + name + " wurde aktualisiert.");
            } else {
                PreparedStatement insertStmt = connection.prepareStatement(
                        "INSERT INTO lebensmittel (produktname, menge, ablaufdatum, datagroup) VALUES (?, ?, ?, ?)");
                insertStmt.setString(1, name);
                insertStmt.setInt(2, 1);
                insertStmt.setDate(3, sqlDate);
                insertStmt.setString(4, datagroup);
                insertStmt.executeUpdate();
                return ResponseEntity.ok("Produkt " + name + " wurde hinzugefügt.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fehler: " + e.getMessage());
        }
    }

    // TODO: Need to be converted to work with datagroups
    @PostMapping("/addEAN")
    public ResponseEntity<String> addEAN(@RequestParam String ean, @RequestParam String name) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement insertStmt = connection.prepareStatement(
                    "INSERT INTO ean_mapping (ean, product_name) VALUES (?, ?)");
            insertStmt.setString(1, ean);
            insertStmt.setString(2, name);
            insertStmt.executeUpdate();
            return ResponseEntity.ok("Produkt " + ean + " wurde hinzugefügt.");
        } catch (SQLException e) {
            // Fehlercode 1062 = Duplicate entry (MySQL)
            if (e.getErrorCode() == 1062) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Fehler: Produktname oder EAN bereits vorhanden.");
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("SQL-Fehler: " + e.getMessage());
        }
    }


    @DeleteMapping("/removeProduct")
    public ResponseEntity<String> removeProduct(@RequestParam String ean, @RequestParam int id, @RequestParam String token) {
        if (userService.checkToken(token, id)) return null;
        ResponseEntity<String> lookupResponse = lookupProductName(ean);
        String name = lookupResponse.getBody();
        String datagroup = userService.getDatagroup(id);

        if (lookupResponse.getStatusCode() != HttpStatus.OK || name == null || name.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Produktname konnte nicht gefunden werden.");
        }

        try (Connection connection = dataSource.getConnection()) {
            String checkQuery = "SELECT menge, ablaufdatum FROM lebensmittel WHERE produktname = ? AND datagroup = ? ORDER BY ablaufdatum ASC LIMIT 1";
            PreparedStatement pstmt = connection.prepareStatement(checkQuery);
            pstmt.setString(1, name);
            pstmt.setString(2, datagroup);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int menge = rs.getInt("menge");
                String ablaufdatum = rs.getString("ablaufdatum");

                if (menge > 1) {
                    String updateQuery = "UPDATE lebensmittel SET menge = ? WHERE produktname = ? AND ablaufdatum = ? AND datagroup = ?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                        updateStmt.setInt(1, menge - 1);
                        updateStmt.setString(2, name);
                        updateStmt.setString(3, ablaufdatum);
                        updateStmt.setString(4, datagroup);
                        updateStmt.executeUpdate();
                    }
                    return ResponseEntity.ok("Menge für Produkt " + name + " wurde um 1 reduziert.");
                } else {
                    String deleteQuery = "DELETE FROM lebensmittel WHERE produktname = ? AND ablaufdatum = ? AND datagroup = ?";
                    try (PreparedStatement deleteStmt = connection.prepareStatement(deleteQuery)) {
                        deleteStmt.setString(1, name);
                        deleteStmt.setString(2, ablaufdatum);
                        deleteStmt.setString(3, datagroup);
                        deleteStmt.executeUpdate();
                    }
                    return ResponseEntity.ok("Produkt " + name + " wurde entfernt.");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Kein Produkt mit diesem Namen gefunden.");
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fehler beim Entfernen des Produkts: " + e.getMessage());
        }
    }
}
