package de.schnitzel.shelfifyapi.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import org.springframework.data.domain.Persistable
import java.time.Instant


@Entity
@Table(name = "invitation_codes")
class InvitationCodeEntity (
    @Id
    var code: String = "",

    var datagroup: String = "",

    @Column(name = "invited_email")
    var invitedEmail: String? = null,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now()
): Persistable<String> {
    @Transient
    private var newEntity: Boolean = true

    override fun getId(): String = code

    override fun isNew(): Boolean = newEntity

    @PostPersist
    @PostLoad
    fun markNotNew() {
        newEntity = false
    }
}