package com.beat.apis.booking.application.command

import com.beat.apis.booking.exception.BookingApplicationErrorCode
import com.beat.apis.exception.ApiApplicationException
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
            throw ApiApplicationException(BookingApplicationErrorCode.AUTHENTICATION_REQUIRED)
        }
        return guestSessionPort.findUserId(guestSessionToken)
            .orElseThrow { ApiApplicationException(BookingApplicationErrorCode.AUTHENTICATION_REQUIRED) }
    }
}
