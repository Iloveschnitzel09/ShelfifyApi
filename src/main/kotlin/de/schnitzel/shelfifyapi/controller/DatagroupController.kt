package de.schnitzel.shelfifyapi.controller

import de.schnitzel.shelfifyapi.entities.BlockedDatagroupEntity
import de.schnitzel.shelfifyapi.entities.InvitationCodeEntity
import de.schnitzel.shelfifyapi.repositories.BlockedDatagroupsRepository
import de.schnitzel.shelfifyapi.repositories.InvitationCodeRepository
import de.schnitzel.shelfifyapi.repositories.UserRepository
import de.schnitzel.shelfifyapi.util.EmailService
import de.schnitzel.shelfifyapi.util.generateCode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant

@RestController
class DatagroupController (
    val userRepository: UserRepository,
    val blockedDatagroupsRepository: BlockedDatagroupsRepository,
    val invitationCodeRepository: InvitationCodeRepository,
    val emailService: EmailService
) {

    companion object {
        private val CODE_VALIDITY: Duration = Duration.ofMinutes(5)
        private val INVITE_COOLDOWN: Duration = Duration.ofMinutes(5)
        private const val MAX_OPEN_INVITES = 3L
    }

    @Transactional
    @PostMapping("/inviteUser")
    fun inviteUser(
        @AuthenticationPrincipal userId: Int,
        @RequestParam email: String
    ) : ResponseEntity<String> {
        val invitee = email.trim().lowercase()
        val user = userRepository.findWithLockById(userId) ?: throw Exception("User not found")
        val datagroup = user.datagroup

        if(blockedDatagroupsRepository.existsByBlockedEmailAndDatagroup(invitee, datagroup)) {
            return ResponseEntity.status(403).body("Dieser Benutzer ist blockiert und kann nicht eingeladen werden.")
        }

        if(userRepository.findByEmail(invitee) == null) {
            return ResponseEntity.status(404).body("Dieser Benutzer ist nicht registriert.")
        }

        val now = Instant.now()

        if (invitationCodeRepository.existsByDatagroupAndInvitedEmailAndCreatedAtAfter(
                datagroup, invitee, now.minus(INVITE_COOLDOWN)
            )
        ) {
            return ResponseEntity.status(429).body("Dieser Benutzer wurde gerade erst eingeladen. Bitte warte ein paar Minuten.")
        }

        if (invitationCodeRepository.countByDatagroupAndCreatedAtAfter(
                datagroup, now.minus(CODE_VALIDITY)
            ) >= MAX_OPEN_INVITES
        ) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Es sind bereits $MAX_OPEN_INVITES Einladungen offen. Bitte warte, bis eine angenommen wurde oder abläuft.")
        }

        val code = generateCode()

        invitationCodeRepository.save(
            InvitationCodeEntity(
                code = code,
                datagroup = datagroup,
                invitedEmail = invitee,
                createdAt = now
            )
        )

        emailService.sendInvitationEmail(invitee, code)
        return ResponseEntity.ok("Einladung erfolgreich gesendet.")
    }

    @Transactional
    @PostMapping("/blockInvitation")
    fun block(
        @AuthenticationPrincipal userId: Int,
        @RequestParam code: String
    ) : ResponseEntity<String> {
        val user = userRepository.findWithLockById(userId) ?: throw Exception("User not found")

        val invitation = invitationCodeRepository.findByCode(code.trim())
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Einladungscode nicht gefunden.")

        val inviteMail = invitation.invitedEmail!!
        if (invitation.invitedEmail == null || !inviteMail.equals(user.email, ignoreCase = true)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Einladungscode nicht gefunden.")
        }

        if (!blockedDatagroupsRepository.existsByBlockedEmailAndDatagroup(inviteMail, invitation.datagroup)) {
            blockedDatagroupsRepository.save(
                BlockedDatagroupEntity(
                    blockedEmail = inviteMail,
                    datagroup = invitation.datagroup
                )
            )
        }

        invitationCodeRepository.deleteByDatagroupAndInvitedEmail(invitation.datagroup, inviteMail)
        return ResponseEntity.ok("Gruppe blockiert.")
    }

    @Transactional
    @PostMapping("/acceptInvitation")
    fun accept(
        @AuthenticationPrincipal userId: Int,
        @RequestParam code: String
    ) : ResponseEntity<String> {
        val user = userRepository.findWithLockById(userId) ?: throw Exception("User not found")

        val invitation = invitationCodeRepository.findByCode(code.trim())
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Einladungscode nicht gefunden.")

        if(user.email != invitation.invitedEmail) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Sie sind nicht der Empfänger dieser Einladung.")
        }

        user.datagroup = invitation.datagroup
        userRepository.save(user)
        return ResponseEntity.ok("Einladung erfolgreich angenommen.")
    }
}