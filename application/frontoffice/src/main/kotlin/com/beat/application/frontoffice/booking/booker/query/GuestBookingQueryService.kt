package com.beat.application.frontoffice.booking.booker.query

import com.beat.application.frontoffice.booking.booker.result.BookingRetrieveResult
import com.beat.application.frontoffice.exception.translateDomainFailure
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class GuestBookingQueryService(
    private val bookerBookingReader: BookerBookingReader,
    private val clock: Clock,
) {
    fun findGuestBookings(userId: Long): List<BookingRetrieveResult> {
        return translateDomainFailure {
            val today = LocalDate.now(clock)
            bookerBookingReader.findByUserId(userId).map { it.toResult(today) }
        }
    }
}
