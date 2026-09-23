package de.schnitzel.shelfifyapi.repositories

import de.schnitzel.shelfifyapi.entities.BlockedDatagroupEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BlockedDatagroupsRepository : JpaRepository<BlockedDatagroupEntity, Int> {
    fun findByDatagroup(datagroup: String): BlockedDatagroupEntity?
    fun findByBlockedEmail(email: String): BlockedDatagroupEntity?
}