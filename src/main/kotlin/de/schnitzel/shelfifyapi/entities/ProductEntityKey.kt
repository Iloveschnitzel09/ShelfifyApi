package de.schnitzel.shelfifyapi.entities

import java.io.Serializable
import java.time.LocalDate

class ProductEntityKey : Serializable {
    var ean: String = ""
    var ablaufdatum: LocalDate = LocalDate.now()
}