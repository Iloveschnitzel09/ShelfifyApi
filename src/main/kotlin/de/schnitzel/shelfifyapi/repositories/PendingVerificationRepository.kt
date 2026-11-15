package de.schnitzel.shelfifyapi.repositories

import de.schnitzel.shelfifyapi.entities.PendingVerificationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PendingVerificationRepository : JpaRepository<PendingVerificationEntity, Int> {
    fun findByUserId(userId: Int): PendingVerificationEntity?
}