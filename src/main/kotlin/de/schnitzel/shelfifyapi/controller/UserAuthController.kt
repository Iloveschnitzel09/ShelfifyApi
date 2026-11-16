package de.schnitzel.shelfifyapi.controller

import de.schnitzel.shelfifyapi.entities.PendingVerificationEntity
import de.schnitzel.shelfifyapi.repositories.PendingVerificationRepository
import de.schnitzel.shelfifyapi.repositories.UserRepository
import de.schnitzel.shelfifyapi.util.EmailService
import de.schnitzel.shelfifyapi.util.generateVerificationCode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant

private val CODE_VALIDITY: Duration = Duration.ofMinutes(10)
private val RESEND_COOLDOWN: Duration = Duration.ofSeconds(60)
private const val MAX_ATTEMPTS = 5

@RestController
class UserAuthController (
    val emailService: EmailService,
    val userRepository: UserRepository,
    private val pendingRepository: PendingVerificationRepository
){
    @Transactional
    @PostMapping("/requestVerification")
    fun requestVerification(
        @AuthenticationPrincipal userId: Int,
        @RequestParam email: String
    ): ResponseEntity<String> {
        val code = generateVerificationCode()
        val user = userRepository.findWithLockById(userId) ?: throw Exception("User not found")

        val owner = userRepository.findByEmail(email)
        if (owner != null && owner.id != user.id) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("E-Mail ist bereits vergeben")
        }

        val now = Instant.now()
        val pending = pendingRepository.findByUserId(userId) ?: PendingVerificationEntity(userId = userId)
        val lastRequest = user.lastCodeRequest?.toInstant()
            ?: if (pending.id != 0) pending.createdAt else null

        if (lastRequest != null && Duration.between(lastRequest, now) < RESEND_COOLDOWN) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Bitte warten Sie, bevor Sie einen neuen Code anfordern.")
        }

        pending.email = email
        pending.code = code
        pending.createdAt = Instant.now()
        pending.attempts = 0
        pendingRepository.save(pending)

        user.lastCodeRequest = Timestamp.from(now)
        userRepository.save(user)

        emailService.sendVerificationEmail(email, code)
        return ResponseEntity.ok("Email sent successfully.")
    }

    @Transactional(noRollbackFor = [ResponseStatusException::class])
    @PostMapping("/verifyEmail")
    fun verifyEmail(
        @AuthenticationPrincipal userId: Int,
        @RequestParam code: String
    ): ResponseEntity<String> {
        val pending = pendingRepository.findWithLockByUserId(userId)
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No pending verification found.")

        if(Duration.between(pending.createdAt, Instant.now()) > CODE_VALIDITY) {
            pendingRepository.delete(pending)
            return ResponseEntity.status(HttpStatus.GONE).body("Verification code has expired.")
        }

        if(pending.code != code.trim()) {
            pending.attempts++
            if(pending.attempts >= MAX_ATTEMPTS) {
                pendingRepository.delete(pending)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Maximum verification attempts exceeded. Please request a new code.")
            } else {
                pendingRepository.save(pending)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid verification code. Attempts left: ${MAX_ATTEMPTS - pending.attempts}")
            }
        }

        val user = userRepository.findById(userId).orElseThrow { Exception("User not found") }

        val owner = userRepository.findByEmail(pending.email)
        if (owner != null && owner.id != user.id) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("E-Mail ist bereits vergeben")
        }

        user.email = pending.email
        user.verified = true
        userRepository.save(user)
        pendingRepository.delete(pending)

        return ResponseEntity.ok("Email verified successfully.")
    }
}