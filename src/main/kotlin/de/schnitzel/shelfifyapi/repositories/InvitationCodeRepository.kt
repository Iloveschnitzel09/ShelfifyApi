package de.schnitzel.shelfifyapi.repositories

import de.schnitzel.shelfifyapi.entities.InvitationCodeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface InvitationCodeRepository : JpaRepository<InvitationCodeEntity, Int> {
    fun findByCode(code: String): InvitationCodeEntity?
    fun existsByDatagroupAndInvitedEmailAndCreatedAtAfter(datagroup: String, invitedEmail: String, after: Instant): Boolean
    fun countByDatagroupAndCreatedAtAfter(datagroup: String, after: Instant): Long

    fun deleteByDatagroupAndInvitedEmail(datagroup: String, invitedEmail: String)

    @Modifying
    @Query("delete from InvitationCodeEntity c where c.createdAt < :cutoff")
    fun deleteOlderThan(@Param("cutoff") cutoff: Instant): Int
}