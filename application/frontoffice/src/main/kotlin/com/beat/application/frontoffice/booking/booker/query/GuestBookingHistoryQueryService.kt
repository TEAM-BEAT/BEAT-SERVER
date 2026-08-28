package com.beat.application.frontoffice.booking.booker.query

import com.beat.application.frontoffice.booking.booker.query.result.BookingRetrieveResult
import com.beat.application.frontoffice.exception.translateDomainFailure
import java.time.Clock
import java.time.LocalDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GuestBookingHistoryQueryService
internal constructor(
    private val guestBookingHistoryReader: GuestBookingHistoryReader,
    private val clock: Clock,
) {
    fun findGuestBookings(userId: Long): List<BookingRetrieveResult> {
        return translateDomainFailure {
            val today = LocalDate.now(clock)
            guestBookingHistoryReader.findByUserIdForGuestAccess(userId).map { it.toResult(today) }
        }
    }
}
