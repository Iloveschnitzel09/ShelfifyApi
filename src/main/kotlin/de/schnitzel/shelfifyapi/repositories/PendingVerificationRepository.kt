package de.schnitzel.shelfifyapi.repositories

import de.schnitzel.shelfifyapi.entities.PendingVerificationEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface PendingVerificationRepository : JpaRepository<PendingVerificationEntity, Int> {
    fun findByUserId(userId: Int): PendingVerificationEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockByUserId(userId: Int): PendingVerificationEntity?
}