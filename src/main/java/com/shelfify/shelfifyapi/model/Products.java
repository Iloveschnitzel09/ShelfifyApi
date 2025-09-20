package com.shelfify.shelfifyapi.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@IdClass(ProductKey.class)
@Table(name = "lebensmittel")
public class Products {

    @Id
    private String produktname;

    private int menge;

    @Id
    private LocalDate ablaufdatum;

    // Getter und Setter

    public String getProduktname() {
        return produktname;
    }

    public void setProduktname(String produktname) {
        this.produktname = produktname;
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

}
