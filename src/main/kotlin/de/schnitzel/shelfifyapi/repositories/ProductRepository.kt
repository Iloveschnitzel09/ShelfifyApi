package de.schnitzel.shelfifyapi.repositories

import de.schnitzel.shelfifyapi.entities.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ProductRepository : JpaRepository<ProductEntity, Int> {
    fun findByDatagroup(datagroup: String): List<ProductEntity>
    fun findByDatagroupAndAblaufdatumBefore(datagroup: String, date: LocalDate): List<ProductEntity>
}