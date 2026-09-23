package de.schnitzel.shelfifyapi.repositories

import de.schnitzel.shelfifyapi.entities.EanMappingEntity
import org.springframework.data.jpa.repository.JpaRepository

interface EanMappingRepository : JpaRepository<EanMappingEntity, Int> {
    fun findByEan(ean: String): EanMappingEntity?
}