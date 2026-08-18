package com.beat.application.frontoffice.booking.command

import com.beat.application.frontoffice.booking.BookingApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.contracts.auth.guest.GuestSessionPort
import org.springframework.stereotype.Service

@Service
class GuestBookingSessionCommandService(
    private val guestSessionPort: GuestSessionPort,
) {
    fun issue(userId: Long): String = guestSessionPort.issue(userId)

    fun resolveActorUserId(memberId: Long?, guestSessionToken: String?): Long {
        if (memberId != null) return memberId
        if (guestSessionToken.isNullOrBlank()) {
            throw FrontofficeApplicationException(BookingApplicationErrorCode.AUTHENTICATION_REQUIRED)
        }
        return guestSessionPort.findUserId(guestSessionToken)
            .orElseThrow { FrontofficeApplicationException(BookingApplicationErrorCode.AUTHENTICATION_REQUIRED) }
    }
}
