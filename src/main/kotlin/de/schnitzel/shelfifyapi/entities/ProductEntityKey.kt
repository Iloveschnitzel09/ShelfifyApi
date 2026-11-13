package de.schnitzel.shelfifyapi.entities

import java.io.Serializable
import java.time.LocalDate

data class ProductEntityKey(
    val ean: String = "",
    val ablaufdatum: LocalDate = LocalDate.now()
) : Serializable