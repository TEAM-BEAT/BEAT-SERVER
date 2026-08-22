package com.beat.batch.booking.job

import com.beat.application.system.booking.command.TicketCleanupService
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TicketCleanupJob(
    private val ticketCleanupService: TicketCleanupService,
    @param:Value("\${beat.scheduler.owner:false}") private val schedulerOwner: Boolean,
) {
    @Scheduled(cron = "0 0 4 * * ?", scheduler = "maintenanceTaskScheduler")
    fun deleteOldCancelledBookings() {
        if (!schedulerOwner) return
        ticketCleanupService.deleteOldCancelledBookings()
    }
}
