package com.shelfify.shelfifyapi.model;

import java.math.BigInteger;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(ProductKey.class)
@Table(name = "products")
public class Products {

    @Id
    private String ean;

    private int menge;

    @Id
    private LocalDate ablaufdatum;

    private String datagroup;

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    public int getMenge() {
        return menge;
    }

    public void setMenge(int menge) {
        this.menge = menge;
    }

    public LocalDate getAblaufdatum() {
        return ablaufdatum;
    }

    public void setAblaufdatum(LocalDate ablaufdatum) {
        this.ablaufdatum = ablaufdatum;
    }

    public String getDatagroup() {
        return datagroup;
    }

    public void setDatagroup(String datagroup) {
        this.datagroup = datagroup;
    }

}
