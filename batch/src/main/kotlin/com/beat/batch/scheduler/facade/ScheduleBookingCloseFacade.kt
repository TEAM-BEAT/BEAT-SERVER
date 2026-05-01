package com.beat.batch.scheduler.facade

import com.beat.batch.scheduler.application.JobSchedulerService
import org.springframework.stereotype.Component

@Component
class ScheduleBookingCloseFacade(
    private val jobSchedulerService: JobSchedulerService,
) {

    fun reconcilePendingSchedules() {
        jobSchedulerService.reconcilePendingSchedules()
    }
}
