package com.beat.application.system.booking.command

import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TicketCleanupService
internal constructor(
    private val bookingRepository: BookingRepository,
    private val clock: Clock,
) {
    @Transactional
    fun deleteOldCancelledBookings() {
        val cutoff = LocalDateTime.now(clock).minusYears(1)
        val oldCancelledBookings =
            bookingRepository.findByBookingStatusAndCancellationDateBefore(
                BookingStatus.BOOKING_CANCELLED,
                cutoff,
            )
        oldCancelledBookings.chunked(500).forEach { chunk ->
            bookingRepository.deleteAll(chunk)
        }
    }
}
