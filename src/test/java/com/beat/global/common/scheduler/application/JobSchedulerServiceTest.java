package com.beat.global.common.scheduler.application;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import com.beat.domain.schedule.domain.Schedule;

class JobSchedulerServiceTest {

	@Test
	void reconcileKeepsExistingScheduledTaskForPendingSchedule() {
		JobSchedulerTransactionalService transactionalService = mock(JobSchedulerTransactionalService.class);
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		JobSchedulerService jobSchedulerService = new JobSchedulerService(transactionalService, taskScheduler);

		ReflectionTestUtils.setField(jobSchedulerService, "schedulerOwner", true);

		Schedule pendingSchedule = mock(Schedule.class);
		when(pendingSchedule.getId()).thenReturn(1L);
		when(transactionalService.findPendingSchedules()).thenReturn(List.of(pendingSchedule));

		ScheduledFuture<?> existingTask = mock(ScheduledFuture.class);
		when(existingTask.isDone()).thenReturn(false);
		when(existingTask.isCancelled()).thenReturn(false);
		getScheduledTasks(jobSchedulerService).put(1L, existingTask);

		jobSchedulerService.reconcilePendingSchedules();

		verify(transactionalService).findPendingSchedules();
		verify(transactionalService, never()).lockSchedule(anyLong());
		verifyNoInteractions(taskScheduler);
		assertSame(existingTask, getScheduledTasks(jobSchedulerService).get(1L));
	}

	@Test
	void reconcileCancelsScheduledTaskThatIsNoLongerPending() {
		JobSchedulerTransactionalService transactionalService = mock(JobSchedulerTransactionalService.class);
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		JobSchedulerService jobSchedulerService = new JobSchedulerService(transactionalService, taskScheduler);

		ReflectionTestUtils.setField(jobSchedulerService, "schedulerOwner", true);
		when(transactionalService.findPendingSchedules()).thenReturn(List.of());

		ScheduledFuture<?> staleTask = mock(ScheduledFuture.class);
		when(staleTask.isDone()).thenReturn(false);
		getScheduledTasks(jobSchedulerService).put(2L, staleTask);

		jobSchedulerService.reconcilePendingSchedules();

		verify(staleTask).cancel(true);
		assertTrue(getScheduledTasks(jobSchedulerService).isEmpty());
	}

	@SuppressWarnings("unchecked")
	private Map<Long, ScheduledFuture<?>> getScheduledTasks(JobSchedulerService jobSchedulerService) {
		return (Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(jobSchedulerService, "scheduledTasks");
	}
}
