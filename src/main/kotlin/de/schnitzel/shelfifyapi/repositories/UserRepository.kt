package de.schnitzel.shelfifyapi.repositories

import de.schnitzel.shelfifyapi.entities.UserEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface UserRepository : JpaRepository<UserEntity, Int> {
    fun findByEmail(email: String): UserEntity?
    fun findAllByDatagroup(datagroup: String): List<UserEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockById(id: Int): UserEntity?
}