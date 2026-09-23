package de.schnitzel.shelfifyapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Id
import jakarta.persistence.Column

@Entity
@Table(name = "ean_mapping")
class EanMappingEntity {
    @Id
    var ean: String = ""

    @Column(name = "product_name")
    var productName: String = ""

    var datagroup: String? = null
}