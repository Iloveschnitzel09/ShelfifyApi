package com.shelfify.shelfifyapi.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.shelfify.shelfifyapi.ean.EanMapping;
import com.shelfify.shelfifyapi.ean.EanMappingRepository;
import com.shelfify.shelfifyapi.model.Products;
import com.shelfify.shelfifyapi.repository.ProduktRepository;
import com.shelfify.shelfifyapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
    public ResponseEntity<List<Products>> getAllProducts(@RequestParam int id, @RequestParam String token, @RequestParam(defaultValue = "-1") int days) {
        try {
            if (userService.checkToken(token, id)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String datagroup = userService.getDatagroup(id);

            List<Products> products;
            if (days < 0) {
                products = produktRepository.findByDatagroup(
                        datagroup,
                        Sort.by(Sort.Order.asc("ean"), Sort.Order.asc("ablaufdatum")));
            } else {
                LocalDate cutoffDate = LocalDate.now().plusDays(days);
                products = produktRepository.findByAblaufdatumBeforeAndDatagroup(
                        cutoffDate,
                        datagroup,
                        Sort.by(Sort.Order.asc("ean"), Sort.Order.asc("ablaufdatum"))
                );
            }
            for(Products p : products) {
                String ean = p.getEan();
                String name = lookupProductName(ean, id, token).getBody();
                p.setEan(name);
                System.out.println(p.getEan());
            }

            return ResponseEntity.status(HttpStatus.OK).body(products);
        } catch (Exception e) {
            System.out.println("Fehler beim Laden der Produkte: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/lookupProductName")
    public ResponseEntity<String> lookupProductName(@RequestParam String ean, @RequestParam int id, @RequestParam String token) {
        if (userService.checkToken(token, id)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String datagroup = userService.getDatagroup(id);

        try {
            Optional<EanMapping> groupMapping =
                    eanMappingRepository.findByEanAndDatagroup(ean, datagroup);

            if (groupMapping.isPresent()) {
                return ResponseEntity.ok(groupMapping.get().getProductName());
            }

            Optional<EanMapping> globalMapping =
                    eanMappingRepository.findByEanAndDatagroupIsNull(ean);

            return globalMapping.map(eanMapping -> ResponseEntity.ok(eanMapping.getProductName())).orElseGet(() -> fetchAndStoreProductNameFromApi(ean));
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
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } else if (responseCode != 200) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }

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
                newEntry.setDatagroup(null);
                eanMappingRepository.save(newEntry);
                return ResponseEntity.ok(productName);
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ean leer oder nicht vorhanden.");

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }


    @PostMapping("/addProduct")
    public ResponseEntity<String> addProduct(@RequestParam String ean, @RequestParam LocalDate ablaufdatum, @RequestParam int id, @RequestParam String token, @RequestParam(defaultValue = "1") int quantity) {
        if (userService.checkToken(token, id)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String datagroup = userService.getDatagroup(id);

        Optional<Products> exist = produktRepository
                .findByEanAndAblaufdatumAndDatagroup(ean, ablaufdatum, datagroup);

        if (exist.isPresent()) {
            Products product = exist.get();
            product.setMenge(product.getMenge() + quantity);
            produktRepository.save(product);
            return ResponseEntity.status(HttpStatus.OK).build();
        }

        Products product = new Products();
        product.setEan(ean);
        product.setMenge(quantity);
        product.setAblaufdatum(ablaufdatum);
        product.setDatagroup(datagroup);

        produktRepository.save(product);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/addEAN")
    public ResponseEntity<String> addEAN(@RequestParam String ean, @RequestParam String name, @RequestParam int id, @RequestParam String token) {
        if (userService.checkToken(token, id)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String datagroup = userService.getDatagroup(id);

        // Prüfen ob EAN oder Name global oder gruppenspezifisch existiert
        Optional<EanMapping> existing =
                eanMappingRepository.findByEanAndDatagroup(ean, datagroup)
                        .or(() -> eanMappingRepository.findByEanAndDatagroupIsNull(ean))
                        .or(() -> eanMappingRepository.findByProductNameAndDatagroup(name, datagroup))
                        .or(() -> eanMappingRepository.findByProductNameAndDatagroupIsNull(name));

        if (existing.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Neu anlegen
        EanMapping newMapping = new EanMapping();
        newMapping.setEan(ean);
        newMapping.setProductName(name);
        newMapping.setDatagroup(datagroup);

        try {
            eanMappingRepository.save(newMapping);
            return ResponseEntity.ok("Produkt " + ean + " wurde hinzugefügt.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }


    @DeleteMapping("/removeProduct")
    public ResponseEntity<String> removeProduct(@RequestParam String ean, @RequestParam int id, @RequestParam String token, @RequestParam(defaultValue = "1") int quantity) {
        if (userService.checkToken(token, id)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ungültiger Token.");
        String datagroup = userService.getDatagroup(id);

        Optional<EanMapping> groupMapping =
                eanMappingRepository.findByEanAndDatagroup(ean, datagroup);
        Optional<EanMapping> globalMapping =
                eanMappingRepository.findByEanAndDatagroupIsNull(ean);

        if (groupMapping.isEmpty() && globalMapping.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        List<Products> products = produktRepository.findByEanAndDatagroupOrderByAblaufdatumAsc(ean, datagroup);

        if (products.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        Products oldestProduct = products.get(0);

        if (oldestProduct.getMenge() > quantity) {
            oldestProduct.setMenge(oldestProduct.getMenge() - quantity);
            produktRepository.save(oldestProduct);
        } else {
            produktRepository.delete(oldestProduct);
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/renameProduct")
    public ResponseEntity<String> renameProduct(@RequestParam String oldName, @RequestParam String newName, @RequestParam int id, @RequestParam String token) {
        try {
            if (userService.checkToken(token, id))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ungültiger Token.");
            String datagroup = userService.getDatagroup(id);

            Optional<EanMapping> groupMapping =
                    eanMappingRepository.findByProductNameAndDatagroup(oldName, datagroup);

            if (groupMapping.isPresent()) {
                EanMapping mapping = groupMapping.get();
                mapping.setProductName(newName);
                eanMappingRepository.save(mapping);
                return ResponseEntity.ok("Produktname erfolgreich geändert.");
            }

            Optional<EanMapping> globalMap =
                    eanMappingRepository.findByProductNameAndDatagroupIsNull(newName);

            if (globalMap.isPresent()) {
                EanMapping override = new EanMapping();
                override.setEan(globalMap.get().getEan());
                override.setProductName(newName);
                override.setDatagroup(datagroup);
                eanMappingRepository.save(override);
                return ResponseEntity.status(HttpStatus.OK).build();
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.out.println("Fehler beim Umbenennen: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
