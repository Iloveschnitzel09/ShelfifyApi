package com.shelfify.shelfifyapi.model;

import java.io.Serializable;
import java.time.LocalDate;

// Muss equals() und hashCode() korrekt implementieren!
public class ProductKey implements Serializable {
    private String produktname;
    private LocalDate ablaufdatum;

}

