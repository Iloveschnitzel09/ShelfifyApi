package de.schnitzel.shelfifyapi.entities

import jakarta.persistence.*


@Entity
@Table(name = "users")
class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0

    @Column(unique = true)
    var email: String = ""

    var notify: Boolean = false

    var datagroup: String = ""

    @Column(name = "verification_code")
    var verificationCode: String = ""

    var verified: Boolean = false

    var token: String = ""

    @Column(name = "own_datagroup")
    var ownDatagroup: String = ""
}