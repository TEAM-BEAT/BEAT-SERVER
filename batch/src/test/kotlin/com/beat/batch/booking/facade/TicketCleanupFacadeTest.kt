package com.beat.batch.booking.facade

import com.beat.batch.booking.application.TicketCleanupService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class TicketCleanupFacadeTest {

    @Test
    fun `cleanup facade delegates to application service`() {
        val ticketCleanupService = mock(TicketCleanupService::class.java)
        val ticketCleanupFacade = TicketCleanupFacade(ticketCleanupService)

        ticketCleanupFacade.deleteOldCancelledBookings()

        verify(ticketCleanupService).deleteOldCancelledBookings()
    }
}
