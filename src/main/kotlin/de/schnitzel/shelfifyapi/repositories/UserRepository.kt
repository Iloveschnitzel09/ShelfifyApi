package de.schnitzel.shelfifyapi.repositories

import de.schnitzel.shelfifyapi.entities.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, Int> {
    fun findByEmail(email: String): UserEntity?
    fun existsByEmail(email: String): Boolean
}