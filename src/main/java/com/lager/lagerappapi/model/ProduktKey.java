package com.lager.lagerappapi.model;

import java.io.Serializable;
import java.time.LocalDate;

// Muss equals() und hashCode() korrekt implementieren!
public class ProduktKey implements Serializable {
    private String produktname;
    private LocalDate ablaufdatum;

}

