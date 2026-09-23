package de.schnitzel.shelfifyapi.repositories

import de.schnitzel.shelfifyapi.entities.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<ProductEntity, Int> {
    fun findByDatagroup(datagroup: String): List<ProductEntity>
}