package com.beat.application.system.booking.command

import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class TicketCleanupService(
    private val bookingRepository: BookingRepository,
    private val clock: Clock,
) {
    @Transactional
    fun deleteOldCancelledBookings() {
        val cutoff = LocalDateTime.now(clock).minusYears(1)
        val oldCancelledBookings = bookingRepository.findByBookingStatusAndCancellationDateBefore(
            BookingStatus.BOOKING_CANCELLED,
            cutoff,
        )
        oldCancelledBookings.chunked(500).forEach { chunk ->
            bookingRepository.deleteAll(chunk)
        }
    }
}
