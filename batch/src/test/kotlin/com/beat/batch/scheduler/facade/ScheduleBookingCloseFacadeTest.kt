package com.beat.batch.scheduler.facade

import com.beat.batch.scheduler.application.JobSchedulerService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class ScheduleBookingCloseFacadeTest {

    @Test
    fun `schedule close facade delegates reconcile to application service`() {
        val jobSchedulerService = mock(JobSchedulerService::class.java)
        val scheduleBookingCloseFacade = ScheduleBookingCloseFacade(jobSchedulerService)

        scheduleBookingCloseFacade.reconcilePendingSchedules()

        verify(jobSchedulerService).reconcilePendingSchedules()
    }
}
