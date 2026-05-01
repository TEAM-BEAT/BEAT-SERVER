package com.beat.batch.booking.facade

import com.beat.batch.booking.application.TicketCleanupService
import org.springframework.stereotype.Component

@Component
class TicketCleanupFacade(
    private val ticketCleanupService: TicketCleanupService,
) {

    fun deleteOldCancelledBookings() {
        ticketCleanupService.deleteOldCancelledBookings()
    }
}
