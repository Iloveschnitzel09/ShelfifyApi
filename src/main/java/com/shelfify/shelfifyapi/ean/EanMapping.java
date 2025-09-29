package com.shelfify.shelfifyapi.ean;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ean_mapping")
public class EanMapping {

    @Id
    private String ean;

    @Column(name = "product_name")
    private String productName;

    private String datagroup;

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

    public String getDatagroup() {
        return datagroup;
    }

    public void setDatagroup(String datagroup) {
        this.datagroup = datagroup;
    }
}

