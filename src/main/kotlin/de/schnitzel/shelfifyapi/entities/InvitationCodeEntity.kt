package de.schnitzel.shelfifyapi.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
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
)