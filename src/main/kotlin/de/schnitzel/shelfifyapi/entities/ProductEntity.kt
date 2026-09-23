package de.schnitzel.shelfifyapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@IdClass(ProductEntityKey::class)
@Table(name = "products")
class ProductEntity {
    @Id
    var ean: String = ""

    var menge: Int = 0

    @Id
    var ablaufdatum: LocalDate = LocalDate.now()

    var datagroup: String = ""
}