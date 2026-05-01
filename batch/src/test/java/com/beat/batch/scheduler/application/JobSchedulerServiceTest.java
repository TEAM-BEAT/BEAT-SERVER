package com.beat.batch.scheduler.application;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import com.beat.contracts.schedule.ScheduleBookingCloseJobTarget;
import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.schedule.domain.Schedule;

class JobSchedulerServiceTest {

	private static final long SCHEDULE_ID = 1L;
	private static final long PERFORMANCE_ID = 10L;

	private static Performance buildPerformance() {
		return Performance.rehydrate(
			PERFORMANCE_ID, "Test", Genre.BAND, 120,
			"desc", "note", null, null, null,
			"poster.jpg", "Team", "Venue", "Road", "Detail",
			"37.5", "127.0", "010-0000-0000", "2026.05.01~2026.05.01",
			10000, 1, 99L
		);
	}

	@Test
	void registerOrRefreshSchedulesTaskWhenRuntimeOwnsScheduler() {
		JobSchedulerTransactionalService transactionalService = mock(JobSchedulerTransactionalService.class);
		PerformanceRepository performanceRepository = mock(PerformanceRepository.class);
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		JobSchedulerService jobSchedulerService = new JobSchedulerService(transactionalService, performanceRepository,
			taskScheduler);

		ReflectionTestUtils.setField(jobSchedulerService, "schedulerOwner", true);

		Schedule schedule = mock(Schedule.class);
		Schedule lockedSchedule = mock(Schedule.class);
		Performance performance = buildPerformance();
		ScheduledFuture<Object> scheduledFuture = mock(ScheduledFuture.class);

		when(schedule.getId()).thenReturn(SCHEDULE_ID);
		when(lockedSchedule.getId()).thenReturn(SCHEDULE_ID);
		when(lockedSchedule.getPerformanceDate()).thenReturn(LocalDateTime.now().plusDays(1));
		when(lockedSchedule.getPerformanceId()).thenReturn(PERFORMANCE_ID);
		when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(performance));
		when(transactionalService.lockSchedule(SCHEDULE_ID)).thenReturn(Optional.of(lockedSchedule));
		doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

		jobSchedulerService.registerOrRefresh(new ScheduleBookingCloseJobTarget(schedule.getId()));

		verify(transactionalService).lockSchedule(SCHEDULE_ID);
		verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
		assertSame(scheduledFuture, getScheduledTasks(jobSchedulerService).get(SCHEDULE_ID));
	}

	@Test
	void registerOrRefreshDoesNothingWhenRuntimeIsNotSchedulerOwner() {
		JobSchedulerTransactionalService transactionalService = mock(JobSchedulerTransactionalService.class);
		PerformanceRepository performanceRepository = mock(PerformanceRepository.class);
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		JobSchedulerService jobSchedulerService = new JobSchedulerService(transactionalService, performanceRepository,
			taskScheduler);

		ReflectionTestUtils.setField(jobSchedulerService, "schedulerOwner", false);

		Schedule schedule = mock(Schedule.class);
		when(schedule.getId()).thenReturn(SCHEDULE_ID);

		jobSchedulerService.registerOrRefresh(new ScheduleBookingCloseJobTarget(schedule.getId()));

		verifyNoInteractions(transactionalService);
		verifyNoInteractions(taskScheduler);
		assertTrue(getScheduledTasks(jobSchedulerService).isEmpty());
	}

	@Test
	void registerOrRefreshReplacesCompletedExistingTask() {
		JobSchedulerTransactionalService transactionalService = mock(JobSchedulerTransactionalService.class);
		PerformanceRepository performanceRepository = mock(PerformanceRepository.class);
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		JobSchedulerService jobSchedulerService = new JobSchedulerService(transactionalService, performanceRepository,
			taskScheduler);

		ReflectionTestUtils.setField(jobSchedulerService, "schedulerOwner", true);
		Schedule lockedSchedule = mock(Schedule.class);
		Performance performance = buildPerformance();
		ScheduledFuture<?> completedTask = mock(ScheduledFuture.class);
		ScheduledFuture<Object> refreshedTask = mock(ScheduledFuture.class);

		when(completedTask.isDone()).thenReturn(true);
		getScheduledTasks(jobSchedulerService).put(SCHEDULE_ID, completedTask);
		when(lockedSchedule.getId()).thenReturn(SCHEDULE_ID);
		when(lockedSchedule.getPerformanceDate()).thenReturn(LocalDateTime.now().plusDays(1));
		when(lockedSchedule.getPerformanceId()).thenReturn(PERFORMANCE_ID);
		when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(performance));
		when(transactionalService.lockSchedule(SCHEDULE_ID)).thenReturn(Optional.of(lockedSchedule));
		doReturn(refreshedTask).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

		jobSchedulerService.registerOrRefresh(new ScheduleBookingCloseJobTarget(SCHEDULE_ID));

		verify(transactionalService).lockSchedule(SCHEDULE_ID);
		verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
		assertSame(refreshedTask, getScheduledTasks(jobSchedulerService).get(SCHEDULE_ID));
	}

	@Test
	void registerOrRefreshDoesNotScheduleWhenLockIsMissing() {
		JobSchedulerTransactionalService transactionalService = mock(JobSchedulerTransactionalService.class);
		PerformanceRepository performanceRepository = mock(PerformanceRepository.class);
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		JobSchedulerService jobSchedulerService = new JobSchedulerService(transactionalService, performanceRepository,
			taskScheduler);

		ReflectionTestUtils.setField(jobSchedulerService, "schedulerOwner", true);
		when(transactionalService.lockSchedule(SCHEDULE_ID)).thenReturn(Optional.empty());

		jobSchedulerService.registerOrRefresh(new ScheduleBookingCloseJobTarget(SCHEDULE_ID));

		verify(transactionalService).lockSchedule(SCHEDULE_ID);
		verifyNoInteractions(performanceRepository, taskScheduler);
		assertTrue(getScheduledTasks(jobSchedulerService).isEmpty());
	}

	@Test
	void reconcileKeepsExistingScheduledTaskForPendingSchedule() {
		JobSchedulerTransactionalService transactionalService = mock(JobSchedulerTransactionalService.class);
		PerformanceRepository performanceRepository = mock(PerformanceRepository.class);
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		JobSchedulerService jobSchedulerService = new JobSchedulerService(transactionalService, performanceRepository,
			taskScheduler);

		ReflectionTestUtils.setField(jobSchedulerService, "schedulerOwner", true);

		Schedule pendingSchedule = mock(Schedule.class);
		when(pendingSchedule.getId()).thenReturn(SCHEDULE_ID);
		when(transactionalService.findPendingSchedules()).thenReturn(List.of(pendingSchedule));

		ScheduledFuture<?> existingTask = mock(ScheduledFuture.class);
		when(existingTask.isDone()).thenReturn(false);
		when(existingTask.isCancelled()).thenReturn(false);
		getScheduledTasks(jobSchedulerService).put(SCHEDULE_ID, existingTask);

		jobSchedulerService.reconcilePendingSchedules();

		verify(transactionalService).findPendingSchedules();
		verify(transactionalService, never()).lockSchedule(anyLong());
		verifyNoInteractions(taskScheduler);
		assertSame(existingTask, getScheduledTasks(jobSchedulerService).get(SCHEDULE_ID));
	}

	@Test
	void reconcileCancelsScheduledTaskThatIsNoLongerPending() {
		JobSchedulerTransactionalService transactionalService = mock(JobSchedulerTransactionalService.class);
		PerformanceRepository performanceRepository = mock(PerformanceRepository.class);
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		JobSchedulerService jobSchedulerService = new JobSchedulerService(transactionalService, performanceRepository,
			taskScheduler);

		ReflectionTestUtils.setField(jobSchedulerService, "schedulerOwner", true);
		when(transactionalService.findPendingSchedules()).thenReturn(List.of());

		ScheduledFuture<?> staleTask = mock(ScheduledFuture.class);
		when(staleTask.isDone()).thenReturn(false);
		getScheduledTasks(jobSchedulerService).put(2L, staleTask);

		jobSchedulerService.reconcilePendingSchedules();

		verify(staleTask).cancel(true);
		assertTrue(getScheduledTasks(jobSchedulerService).isEmpty());
	}

	@Test
	void reconcileDoesNothingWhenRuntimeIsNotSchedulerOwner() {
		JobSchedulerTransactionalService transactionalService = mock(JobSchedulerTransactionalService.class);
		PerformanceRepository performanceRepository = mock(PerformanceRepository.class);
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		JobSchedulerService jobSchedulerService = new JobSchedulerService(transactionalService, performanceRepository,
			taskScheduler);

		ReflectionTestUtils.setField(jobSchedulerService, "schedulerOwner", false);

		jobSchedulerService.reconcilePendingSchedules();

		verifyNoInteractions(transactionalService, taskScheduler);
	}

	@Test
	void cancelRemovesScheduledTaskWhenRuntimeOwnsScheduler() {
		JobSchedulerTransactionalService transactionalService = mock(JobSchedulerTransactionalService.class);
		PerformanceRepository performanceRepository = mock(PerformanceRepository.class);
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		JobSchedulerService jobSchedulerService = new JobSchedulerService(transactionalService, performanceRepository,
			taskScheduler);

		ReflectionTestUtils.setField(jobSchedulerService, "schedulerOwner", true);
		ScheduledFuture<?> scheduledTask = mock(ScheduledFuture.class);
		when(scheduledTask.isDone()).thenReturn(false);
		getScheduledTasks(jobSchedulerService).put(SCHEDULE_ID, scheduledTask);

		jobSchedulerService.cancel(new ScheduleBookingCloseJobTarget(SCHEDULE_ID));

		verify(scheduledTask).cancel(true);
		assertTrue(getScheduledTasks(jobSchedulerService).isEmpty());
	}

	@Test
	void cancelDoesNothingWhenRuntimeIsNotSchedulerOwner() {
		JobSchedulerTransactionalService transactionalService = mock(JobSchedulerTransactionalService.class);
		PerformanceRepository performanceRepository = mock(PerformanceRepository.class);
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		JobSchedulerService jobSchedulerService = new JobSchedulerService(transactionalService, performanceRepository,
			taskScheduler);

		ReflectionTestUtils.setField(jobSchedulerService, "schedulerOwner", false);
		ScheduledFuture<?> scheduledTask = mock(ScheduledFuture.class);
		getScheduledTasks(jobSchedulerService).put(SCHEDULE_ID, scheduledTask);

		jobSchedulerService.cancel(new ScheduleBookingCloseJobTarget(SCHEDULE_ID));

		verifyNoInteractions(scheduledTask);
		assertSame(scheduledTask, getScheduledTasks(jobSchedulerService).get(SCHEDULE_ID));
	}

	@Test
	void updateIsBookingFalseClosesBookingAndRemovesCompletedTask() {
		JobSchedulerTransactionalService transactionalService = mock(JobSchedulerTransactionalService.class);
		PerformanceRepository performanceRepository = mock(PerformanceRepository.class);
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		JobSchedulerService jobSchedulerService = new JobSchedulerService(transactionalService, performanceRepository,
			taskScheduler);

		ScheduledFuture<?> scheduledTask = mock(ScheduledFuture.class);
		getScheduledTasks(jobSchedulerService).put(SCHEDULE_ID, scheduledTask);

		jobSchedulerService.updateIsBookingFalse(SCHEDULE_ID);

		verify(transactionalService).closeBooking(SCHEDULE_ID);
		assertTrue(getScheduledTasks(jobSchedulerService).isEmpty());
	}

	@SuppressWarnings("unchecked")
	private Map<Long, ScheduledFuture<?>> getScheduledTasks(JobSchedulerService jobSchedulerService) {
		Map<Long, ScheduledFuture<?>> scheduledTasks =
			(Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(jobSchedulerService, "scheduledTasks");
		assertNotNull(scheduledTasks);
		return scheduledTasks;
	}
}
