package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
internal class GuestBookingSessionManager(private val guestSessionStore: GuestSessionStore) {
    fun issueOrNull(userId: Long): String? =
        try {
            translateDomainFailure { guestSessionStore.issue(userId) }
        } catch (exception: RuntimeException) {
            log.error(exception) {
                "Guest session issuance failed after successful booking flow: userId=$userId"
            }
            null
        }

    fun resolveActorUserId(actor: BookingActorCommand): Long = translateDomainFailure {
        actor.memberId
            ?: actor.guestSessionToken
                ?.takeIf(String::isNotBlank)
                ?.let(guestSessionStore::findUserId)
            ?: throw FrontofficeApplicationException(
                BookingApplicationErrorCode.AUTHENTICATION_REQUIRED
            )
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
