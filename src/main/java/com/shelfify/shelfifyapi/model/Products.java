package com.shelfify.shelfifyapi.model;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@JsonInclude(JsonInclude.Include.NON_NULL)
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

    @Override
    public String toString() {
        return "Products{" +
                "ean='" + ean + '\'' +
                ", menge=" + menge +
                ", ablaufdatum=" + ablaufdatum +
                ", datagroup='" + datagroup + '\'' +
                '}';
    }

    public Products(String name, int menge, LocalDate ablaufdatum, String datagroup) {
        this.ean = name;
        this.menge = menge;
        this.ablaufdatum = ablaufdatum;
        this.datagroup = datagroup;
    }

    public Products() {}
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
