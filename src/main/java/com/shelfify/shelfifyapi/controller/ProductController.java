package com.shelfify.shelfifyapi.controller;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shelfify.shelfifyapi.ean.EanMapping;
import com.shelfify.shelfifyapi.ean.EanMappingRepository;
import com.shelfify.shelfifyapi.model.Products;
import com.shelfify.shelfifyapi.repository.ProduktRepository;
import com.shelfify.shelfifyapi.service.UserService;

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
    public ResponseEntity<List<Products>> getAllProducts(@RequestParam int id, @RequestParam String token) {
        try {
            if (userService.checkToken(token, id)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<Products> products = produktRepository.findByDatagroup(
                    userService.getDatagroup(id),
                    Sort.by(Sort.Order.asc("produktname"), Sort.Order.asc("ablaufdatum"))
            );
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            System.out.println("Fehler beim Laden der Produkte: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/spoiledProducts")
    public ResponseEntity<List<Products>> getSpoiledProducts(@RequestParam(defaultValue = "10") int days, @RequestParam int id, @RequestParam String token) {
        try {
            if (userService.checkToken(token, id)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            LocalDate cutoffDate = LocalDate.now().plusDays(days);
            List<Products> spoiledProducts = produktRepository.findByAblaufdatumBeforeAndDatagroup(
                    cutoffDate,
                    userService.getDatagroup(id),
                    Sort.by(Sort.Order.asc("produktname"), Sort.Order.asc("ablaufdatum"))
            );
            return ResponseEntity.ok(spoiledProducts);
        } catch (Exception e) {
            System.out.println("Fehler beim Laden der abgelaufenen Produkte: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/lookupProductName")
    public ResponseEntity<String> lookupProductName(@RequestParam String ean, @RequestParam int id, @RequestParam String token) {
        if (userService.checkToken(token, id)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            Optional<EanMapping> globalMapping = eanMappingRepository.findByEanAndDatagroupIsNull(ean);
            if (globalMapping.isPresent()) {
                return ResponseEntity.ok(globalMapping.get().getProductName());
            }

            String datagroup = userService.getDatagroup(id);
            Optional<EanMapping> groupMapping = eanMappingRepository.findByEanAndDatagroup(ean, datagroup);
            return groupMapping.map(eanMapping -> ResponseEntity.ok(eanMapping.getProductName())).orElseGet(() -> fetchAndStoreProductNameFromApi(ean));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<String> fetchAndStoreProductNameFromApi(String ean) {
        try {
            String apiUrl = "https://world.openfoodfacts.org/api/v2/product/" + ean + ".json";
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == 404) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Produkt nicht gefunden.");
            } else if (responseCode != 200) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body("Fehler von OpenFoodFacts: " + responseCode);
            }

            // Nur wenn 200 OK
            String jsonResponse = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)
                    .useDelimiter("\\A").next();

            String productName = new ObjectMapper()
                    .readTree(jsonResponse)
                    .path("product")
                    .path("product_name")
                    .asText();

            if (productName != null && !productName.isEmpty()) {
                EanMapping newEntry = new EanMapping();
                newEntry.setEan(ean);
                newEntry.setProductName(productName);
                eanMappingRepository.save(newEntry);
                return ResponseEntity.ok(productName);
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Produktname leer oder nicht vorhanden.");

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/addProduct")
    public ResponseEntity<String> addProduct(@RequestParam String name, @RequestParam String ablaufdatum, @RequestParam int id, @RequestParam String token, @RequestParam(defaultValue = "1") int quantity) {
        if (userService.checkToken(token, id)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String datagroup = userService.getDatagroup(id);
        try (Connection connection = dataSource.getConnection()) {
            java.sql.Date sqlDate = java.sql.Date.valueOf(ablaufdatum);

            String checkQuery = "SELECT menge FROM products WHERE produktname = ? AND ablaufdatum = ? AND datagroup = ?";
            PreparedStatement pstmt = connection.prepareStatement(checkQuery);
            pstmt.setString(1, name);
            pstmt.setDate(2, sqlDate);
            pstmt.setString(3, datagroup);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int currentMenge = rs.getInt("menge");
                PreparedStatement updateStmt = connection.prepareStatement(
                        "UPDATE products SET menge = ? WHERE produktname = ? AND ablaufdatum = ? AND datagroup = ?");
                updateStmt.setInt(1, currentMenge + quantity);
                updateStmt.setString(2, name);
                updateStmt.setDate(3, sqlDate);
                updateStmt.setString(4, datagroup);
                updateStmt.executeUpdate();
                return ResponseEntity.ok().build();
            } else {
                PreparedStatement insertStmt = connection.prepareStatement("INSERT INTO products (produktname, menge, ablaufdatum, datagroup) VALUES (?, ?, ?, ?)");
                insertStmt.setString(1, name);
                insertStmt.setInt(2, quantity);
                insertStmt.setDate(3, sqlDate);
                insertStmt.setString(4, datagroup);
                insertStmt.executeUpdate();
                return ResponseEntity.ok().build();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/addEAN")
    public ResponseEntity<String> addEAN(@RequestParam String ean, @RequestParam String name, @RequestParam int id, @RequestParam String token) {
        if (userService.checkToken(token, id)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String datagroup = userService.getDatagroup(id);

        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement checkStmt = connection.prepareStatement(
                    "SELECT * FROM ean_mapping WHERE (ean = ? OR product_name = ?) AND (datagroup = ? or datagroup IS NULL)");
            checkStmt.setString(1, ean);
            checkStmt.setString(2, name);
            checkStmt.setString(3, datagroup);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) return ResponseEntity.status(HttpStatus.CONFLICT).build();

            PreparedStatement insertStmt = connection.prepareStatement(
                    "INSERT INTO ean_mapping (ean, product_name, datagroup) VALUES (?, ?, ?)");
            insertStmt.setString(1, ean);
            insertStmt.setString(2, name);
            insertStmt.setString(3, datagroup);
            insertStmt.executeUpdate();
            return ResponseEntity.ok("Produkt " + ean + " wurde hinzugefügt.");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    @DeleteMapping("/removeProduct")
    public ResponseEntity<String> removeProduct(@RequestParam String ean, @RequestParam int id, @RequestParam String token, @RequestParam(defaultValue = "1") int quantity) {
        if (userService.checkToken(token, id)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ungültiger Token.");

        String name = null;
        String datagroup = userService.getDatagroup(id);
        Optional<EanMapping> globalMapping = eanMappingRepository.findByEanAndDatagroupIsNull(ean);
        if (globalMapping.isPresent()) {
            name =globalMapping.get().getProductName();
        }

        Optional<EanMapping> groupMapping = eanMappingRepository.findByEanAndDatagroup(ean, datagroup);
        if (groupMapping.isPresent()) {
            name = groupMapping.get().getProductName();
        }

        if (name == null || name.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Produktname konnte nicht gefunden werden.");
        }

        try (Connection connection = dataSource.getConnection()) {
            String checkQuery = "SELECT menge, ablaufdatum FROM products WHERE produktname = ? AND datagroup = ? ORDER BY ablaufdatum ASC LIMIT 1";
            PreparedStatement pstmt = connection.prepareStatement(checkQuery);
            pstmt.setString(1, name);
            pstmt.setString(2, datagroup);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int menge = rs.getInt("menge");
                String ablaufdatum = rs.getString("ablaufdatum");

                if (menge > quantity) {
                    String updateQuery = "UPDATE products SET menge = ? WHERE produktname = ? AND ablaufdatum = ? AND datagroup = ?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                        updateStmt.setInt(1, menge - quantity);
                        updateStmt.setString(2, name);
                        updateStmt.setString(3, ablaufdatum);
                        updateStmt.setString(4, datagroup);
                        updateStmt.executeUpdate();
                    }
                    return ResponseEntity.ok("Menge für Produkt " + name + " wurde um 1 reduziert.");
                } else {
                    String deleteQuery = "DELETE FROM products WHERE produktname = ? AND ablaufdatum = ? AND datagroup = ?";
                    try (PreparedStatement deleteStmt = connection.prepareStatement(deleteQuery)) {
                        deleteStmt.setString(1, name);
                        deleteStmt.setString(2, ablaufdatum);
                        deleteStmt.setString(3, datagroup);
                        deleteStmt.executeUpdate();
                    }
                    return ResponseEntity.ok("Produkt " + name + " wurde entfernt.");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/renameProduct")
    public ResponseEntity<String> renameProduct(@RequestParam String oldName, @RequestParam String newName, @RequestParam int id, @RequestParam String token) {
        try {
            if (userService.checkToken(token, id)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ungültiger Token.");
            }
            String datagroup = userService.getDatagroup(id);
            
            // Prüfen ob das alte Produkt existiert
            try (Connection connection = dataSource.getConnection()) {
                String checkGroupQuery = "SELECT * FROM ean_mapping WHERE product_name = ? AND datagroup = ?";
                PreparedStatement checkOldStmt = connection.prepareStatement(checkGroupQuery);
                checkOldStmt.setString(1, oldName);
                checkOldStmt.setString(2, datagroup);
                ResultSet rsGroup = checkOldStmt.executeQuery();
                
                if (rsGroup.next() && rsGroup.getInt(1) == 0) {
                    String checkPubQuery = "SELECT * FROM ean_mapping WHERE product_name = ? AND datagroup IS NULL";
                    PreparedStatement checkPubStmt = connection.prepareStatement(checkPubQuery);
                    checkPubStmt.setString(1, oldName);
                    ResultSet rsPub = checkPubStmt.executeQuery();

                    if (rsPub.next() && rsPub.getInt(1) == 0) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                    }

                    String createGroupQuery = "INSERT INTO ean_mapping (ean, product_name, datagroup) VALUES (?, ?, ?)";
                    PreparedStatement createGroupStmt = connection.prepareStatement(createGroupQuery);
                    createGroupStmt.setString(1, rsPub.getString("ean"));
                    createGroupStmt.setString(2, newName);
                    createGroupStmt.setString(3, datagroup);
                    createGroupStmt.executeUpdate();

                    return ResponseEntity.ok().build();
                } else {
                    String updateGroupQuery = "UPDATE ean_mapping SET product_name = ? WHERE product_name = ? AND datagroup = ?";
                    PreparedStatement updateGroupStmt = connection.prepareStatement(updateGroupQuery);
                    updateGroupStmt.setString(1, newName);
                    updateGroupStmt.setString(2, oldName);
                    updateGroupStmt.setString(3, datagroup);
                    updateGroupStmt.executeUpdate();
                }
                return ResponseEntity.ok().build();
            }
        } catch (SQLException e) {
            System.out.println("SQL-Fehler beim Umbenennen: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            System.out.println("Fehler beim Umbenennen: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
