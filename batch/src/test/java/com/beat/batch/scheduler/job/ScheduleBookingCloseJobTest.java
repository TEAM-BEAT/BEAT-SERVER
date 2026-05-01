package com.beat.batch.scheduler.job;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

import com.beat.batch.scheduler.application.JobSchedulerService;

class ScheduleBookingCloseJobTest {

	@Test
	void applicationReadyKeepsEventListenerContract() throws NoSuchMethodException {
		EventListener eventListener = ScheduleBookingCloseJob.class
			.getDeclaredMethod("onApplicationReady")
			.getAnnotation(EventListener.class);

		assertArrayEquals(new Class[] {ApplicationReadyEvent.class}, eventListener.value());
	}

	@Test
	void scheduledReconcileKeepsFixedDelayContract() throws NoSuchMethodException {
		Scheduled scheduled = ScheduleBookingCloseJob.class
			.getDeclaredMethod("reconcilePendingSchedules")
			.getAnnotation(Scheduled.class);

		assertEquals("${beat.scheduler.reconcile-interval-ms:60000}", scheduled.fixedDelayString());
	}

	@Test
	void applicationReadyRehydratesSchedulesWhenRuntimeOwnsScheduler() {
		JobSchedulerService jobSchedulerService = mock(JobSchedulerService.class);
		ScheduleBookingCloseJob scheduleBookingCloseJob = new ScheduleBookingCloseJob(jobSchedulerService);
		ReflectionTestUtils.setField(scheduleBookingCloseJob, "schedulerOwner", true);

		scheduleBookingCloseJob.onApplicationReady();

		verify(jobSchedulerService).reconcilePendingSchedules();
	}

	@Test
	void applicationReadySkipsRehydrationWhenRuntimeIsNotSchedulerOwner() {
		JobSchedulerService jobSchedulerService = mock(JobSchedulerService.class);
		ScheduleBookingCloseJob scheduleBookingCloseJob = new ScheduleBookingCloseJob(jobSchedulerService);
		ReflectionTestUtils.setField(scheduleBookingCloseJob, "schedulerOwner", false);

		scheduleBookingCloseJob.onApplicationReady();

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
