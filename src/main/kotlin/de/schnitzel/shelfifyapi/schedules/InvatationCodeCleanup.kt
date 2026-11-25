package de.schnitzel.shelfifyapi.schedules

import de.schnitzel.shelfifyapi.repositories.InvitationCodeRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

@Component
class InvitationCodeCleanupJob(
    private val invitationCodeRepository: InvitationCodeRepository
) {

    companion object {
        private val RETENTION: Duration = Duration.ofHours(1)
    }

    @Scheduled(initialDelay = 1, fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    @Transactional
    fun deleteExpiredCodes() {
        val deleted = invitationCodeRepository.deleteOlderThan(Instant.now().minus(RETENTION))
        if (deleted > 0) {
            println("{} abgelaufene Einladungscodes gelöscht")
        }
    }
}