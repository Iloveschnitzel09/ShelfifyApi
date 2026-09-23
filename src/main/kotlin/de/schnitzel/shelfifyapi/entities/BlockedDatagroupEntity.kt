package de.schnitzel.shelfifyapi.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "blocked_datagroups")
class BlockedDatagroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0

    var datagroup: String = ""

    @Column(name = "blocked_email")
    var blockedEmail: String = ""
}