package de.schnitzel.shelfifyapi.repositories

import de.schnitzel.shelfifyapi.entities.EanMappingEntity
import org.springframework.data.jpa.repository.JpaRepository

interface EanMappingRepository : JpaRepository<EanMappingEntity, Int> {
    fun findByEan(ean: String): EanMappingEntity?
    fun findAllByDatagroup(datagroup: String): List<EanMappingEntity>
    fun findByEanAndDatagroup(ean: String, datagroup: String): EanMappingEntity?
    fun findByEanAndDatagroupIsNull(ean: String): EanMappingEntity?
    fun findByProductNameAndDatagroup(productName: String, datagroup: String): EanMappingEntity?
    fun findByProductNameAndDatagroupIsNull(productName: String): EanMappingEntity?
}