package de.schnitzel.shelfifyapi.repositories

import de.schnitzel.shelfifyapi.entities.InvitationCodeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface InvitationCodeRepository : JpaRepository<InvitationCodeEntity, Int> {
    fun findByCode(code: String): InvitationCodeEntity?
    fun existsByCode(code: String): Boolean
}