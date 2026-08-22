package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import org.springframework.stereotype.Service

@Service
class GuestBookingSessionCommandService(
    private val guestSessionStore: GuestSessionStore,
) {
    fun issue(userId: Long): String = translateDomainFailure { guestSessionStore.issue(userId) }

    fun resolveActorUserId(memberId: Long?, guestSessionToken: String?): Long =
        translateDomainFailure {
            if (memberId != null) {
                memberId
            } else {
                if (guestSessionToken.isNullOrBlank()) {
                    throw FrontofficeApplicationException(BookingApplicationErrorCode.AUTHENTICATION_REQUIRED)
                }
                guestSessionStore.findUserId(guestSessionToken)
                    ?: throw FrontofficeApplicationException(BookingApplicationErrorCode.AUTHENTICATION_REQUIRED)
            }
        }
}
