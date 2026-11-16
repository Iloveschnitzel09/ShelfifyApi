package de.schnitzel.shelfifyapi.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "pending_verifications")
class PendingVerificationEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,

    @Column(name = "user_id", unique = true, nullable = false)
    var userId: Int = 0,

    @Column(nullable = false)
    var email: String = "",

    @Column(nullable = false)
    var code: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var attempts: Int = 0
)