package com.beat.apps.batch.booking.job

import com.beat.application.system.booking.command.TicketCleanupService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TicketCleanupJob(private val ticketCleanupService: TicketCleanupService) {
    @Scheduled(cron = "0 0 4 * * ?", scheduler = "maintenanceTaskScheduler")
    fun deleteOldCancelledBookings() {
        ticketCleanupService.deleteOldCancelledBookings()
    }
}
