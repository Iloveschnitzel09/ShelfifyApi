package com.lager.lagerappapi.ean;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ean_mapping")
public class EanMapping {

    @Id
    private String ean; // EAN wird als Primärschlüssel benutzt

    @Column(name = "product_name")
    private String productName;

    // Getter und Setter
    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }
}

