package com.beat.batch.scheduler.job;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.test.util.ReflectionTestUtils;

import com.beat.batch.scheduler.application.JobSchedulerService;

class ScheduleBookingCloseJobTest {

	@Test
	void applicationReadyRehydratesSchedulesWhenRuntimeOwnsScheduler() {
		JobSchedulerService jobSchedulerService = mock(JobSchedulerService.class);
		ScheduleBookingCloseJob scheduleBookingCloseJob = new ScheduleBookingCloseJob(jobSchedulerService);
		ReflectionTestUtils.setField(scheduleBookingCloseJob, "schedulerOwner", true);

		scheduleBookingCloseJob.onApplicationReady(mock(ApplicationReadyEvent.class));

		verify(jobSchedulerService).reconcilePendingSchedules();
	}

	@Test
	void applicationReadySkipsRehydrationWhenRuntimeIsNotSchedulerOwner() {
		JobSchedulerService jobSchedulerService = mock(JobSchedulerService.class);
		ScheduleBookingCloseJob scheduleBookingCloseJob = new ScheduleBookingCloseJob(jobSchedulerService);
		ReflectionTestUtils.setField(scheduleBookingCloseJob, "schedulerOwner", false);

		scheduleBookingCloseJob.onApplicationReady(mock(ApplicationReadyEvent.class));

		verifyNoInteractions(jobSchedulerService);
	}

	@Test
	void scheduledReconcileDelegatesWhenRuntimeOwnsScheduler() {
		JobSchedulerService jobSchedulerService = mock(JobSchedulerService.class);
		ScheduleBookingCloseJob scheduleBookingCloseJob = new ScheduleBookingCloseJob(jobSchedulerService);
		ReflectionTestUtils.setField(scheduleBookingCloseJob, "schedulerOwner", true);

		scheduleBookingCloseJob.reconcilePendingSchedules();

		verify(jobSchedulerService).reconcilePendingSchedules();
	}

	@Test
	void scheduledReconcileSkipsWhenRuntimeIsNotSchedulerOwner() {
		JobSchedulerService jobSchedulerService = mock(JobSchedulerService.class);
		ScheduleBookingCloseJob scheduleBookingCloseJob = new ScheduleBookingCloseJob(jobSchedulerService);
		ReflectionTestUtils.setField(scheduleBookingCloseJob, "schedulerOwner", false);

		scheduleBookingCloseJob.reconcilePendingSchedules();

		verifyNoInteractions(jobSchedulerService);
	}
}
