package de.schnitzel.shelfifyapi.entities

import jakarta.persistence.*
import java.sql.Timestamp

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,

    @Column(unique = true)
    var email: String? = null,

    var notify: Boolean = false,

    var datagroup: String = "",

    @Column(name = "verification_code")
    var verificationCode: String? = null,

    @Column(name = "last_code_request", nullable = true)
    var lastCodeRequest: Timestamp? = null,

    var verified: Boolean = false,

    @Column(name = "token", length = 1024)
    var refreshToken: String? = null,

    @Column(name = "own_datagroup")
    var ownDatagroup: String = ""
)