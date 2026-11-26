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

data class GroupMembersResponse(
    val owner: Boolean,
    val members: List<String>
)

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
        private const val MAX_CODE_ATTEMPTS = 5
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

        val code = generateUniqueCode()

        invitationCodeRepository.save(
            InvitationCodeEntity(
                code = code,
                datagroup = datagroup,
                invitedEmail = invitee,
                createdAt = now
            )
        )

        try {
            emailService.sendInvitationEmail(invitee, code)
        } catch (e: Exception) {
            print("Einladungs-E-Mail konnte nicht gesendet werden $e")
            invitationCodeRepository.deleteById(code.toInt())
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("Die Einladung konnte nicht per E-Mail gesendet werden. Bitte versuche es später erneut.")
        }
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

        val inviteMail = invitation.invitedEmail
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Einladungscode nicht gefunden.")

        if (!inviteMail.equals(user.email, ignoreCase = true)) {
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

        val invitedEmail = invitation.invitedEmail
        if (invitedEmail == null || !invitedEmail.equals(user.email, ignoreCase = true)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Sie sind nicht der Empfänger dieser Einladung.")
        }

        if (invitation.createdAt.isBefore(Instant.now().minus(CODE_VALIDITY))) {
            return ResponseEntity.status(HttpStatus.GONE).body("Der Einladungscode ist abgelaufen.")
        }

        user.datagroup = invitation.datagroup
        userRepository.save(user)

        invitationCodeRepository.delete(invitation)
        return ResponseEntity.ok("Einladung erfolgreich angenommen.")
    }

    @Transactional
    @PostMapping("/leaveGroup")
    fun leaveGroup(
        @AuthenticationPrincipal userId: Int
    ) : ResponseEntity<String> {
        val user = userRepository.findWithLockById(userId) ?: throw Exception("User not found")
        user.datagroup = user.ownDatagroup
        userRepository.save(user)
        return ResponseEntity.ok("Gruppe erfolgreich verlassen.")
    }

    @PostMapping("/groupMembers")
    fun getGroupMembers(
        @AuthenticationPrincipal userId: Int
    ): ResponseEntity<GroupMembersResponse> {
        val user = userRepository.findById(userId).orElseThrow { Exception("User not found") }
        val members = userRepository.findAllByDatagroup(user.datagroup).mapNotNull { it.email }
        return ResponseEntity.ok(
            GroupMembersResponse(
                owner = user.datagroup == user.ownDatagroup,
                members = members
            )
        )
    }

    @Transactional
    @PostMapping("/kickMember")
    fun kickMember(
        @AuthenticationPrincipal userId: Int,
        @RequestParam email: String
    ) : ResponseEntity<String> {
        val user = userRepository.findWithLockById(userId) ?: throw Exception("User not found")
        if(user.datagroup != user.ownDatagroup) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Nur der Gruppenbesitzer kann Mitglieder entfernen.")
        }

        val target = userRepository.findByEmail(email.trim().lowercase())
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Benutzer nicht gefunden.")

        val member = userRepository.findWithLockById(target.id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Benutzer nicht gefunden.")

        if (member.datagroup != user.datagroup) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Benutzer ist nicht in Ihrer Gruppe.")
        }

        member.datagroup = member.ownDatagroup
        userRepository.save(member)
        return ResponseEntity.ok("Benutzer erfolgreich aus der Gruppe entfernt.")
    }

    private fun generateUniqueCode(): String {
        repeat(MAX_CODE_ATTEMPTS) {
            val candidate = generateCode()
            if (!invitationCodeRepository.existsById(candidate.toInt())) {
                return candidate
            }
        }
        throw IllegalStateException("Es konnte kein eindeutiger Einladungscode erzeugt werden.")
    }
}