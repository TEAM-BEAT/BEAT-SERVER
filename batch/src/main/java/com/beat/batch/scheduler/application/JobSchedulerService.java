package com.beat.batch.scheduler.application;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.beat.contracts.schedule.ScheduleBookingCloseJobPort;
import com.beat.contracts.schedule.ScheduleBookingCloseJobTarget;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.repository.PerformanceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSchedulerService implements ScheduleBookingCloseJobPort {

	private final JobSchedulerTransactionalService jobSchedulerTransactionalService;
	private final PerformanceRepository performanceRepository;
	private final TaskScheduler taskScheduler;

	@Value("${beat.scheduler.owner:false}")
	private boolean schedulerOwner;

	// 스케줄 ID와 관련된 작업을 관리하기 위한 ConcurrentHashMap 선언
	private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

	public void reconcilePendingSchedules() {
		if (!schedulerOwner) {
			return;
		}

		List<Long> pendingScheduleIds = jobSchedulerTransactionalService.findPendingScheduleIds();
		pendingScheduleIds.forEach(scheduleId -> registerOrRefresh(new ScheduleBookingCloseJobTarget(scheduleId)));

		new ArrayList<>(scheduledTasks.keySet()).stream()
			.filter(scheduleId -> !pendingScheduleIds.contains(scheduleId))
			.forEach(this::cancelScheduledTaskById);
	}

	@Override
	public void registerOrRefresh(ScheduleBookingCloseJobTarget target) {
		if (!schedulerOwner) {
			log.info("Ignoring schedule registration because this runtime is not the scheduler owner.");
			return;
		}

		ScheduledFuture<?> existingTask = scheduledTasks.get(target.scheduleId());
		if (existingTask != null) {
			if (!existingTask.isDone() && !existingTask.isCancelled()) {
				log.debug("Schedule ID {} is already scheduled. Skipping duplicate registration.", target.scheduleId());
				return;
			}
			scheduledTasks.remove(target.scheduleId());
		}

		schedulePendingTask(target);
	}

	// 스케줄 종료 시 isBooking을 false로 업데이트
	public void updateIsBookingFalse(Long scheduleId) {
		log.info("Updating isBooking to false for schedule ID: {}", scheduleId);
		jobSchedulerTransactionalService.closeBooking(scheduleId);

		// 스케줄 작업 완료 후 Map에서 삭제
		scheduledTasks.remove(scheduleId);
		log.debug("Completed Task removed for Schedule ID: {}", scheduleId);
		logScheduledTasks();
	}

	@Override
	public void cancel(ScheduleBookingCloseJobTarget target) {
		if (!schedulerOwner) {
			log.info("Ignoring schedule cancellation because this runtime is not the scheduler owner.");
			return;
		}

		cancelScheduledTaskById(target.scheduleId());
	}

	private void schedulePendingTask(ScheduleBookingCloseJobTarget target) {
		// 여기서 데이터베이스 X-Lock을 걸어 중복 실행 방지
		jobSchedulerTransactionalService.lockScheduleBookingWindow(target.scheduleId())
			.ifPresentOrElse(
				lockedSchedule -> {
					log.info("Lock acquired for Schedule ID: {}", lockedSchedule.scheduleId());
					Performance performance = performanceRepository.findById(lockedSchedule.performanceId())
						.orElseThrow(() -> new IllegalStateException(
							"Performance not found for schedule " + lockedSchedule.scheduleId()));
					LocalDateTime performanceEndTime = lockedSchedule.performanceDate()
						.plusMinutes(performance.getRunningTime());

					log.info("Scheduling task for Schedule ID: {} at {}", lockedSchedule.scheduleId(),
						performanceEndTime);

					ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
						() -> updateIsBookingFalse(lockedSchedule.scheduleId()),
						performanceEndTime.atZone(ZoneId.systemDefault()).toInstant()
					);

					scheduledTasks.put(lockedSchedule.scheduleId(), scheduledTask);
					log.debug("Task added for Schedule ID: {}", lockedSchedule.scheduleId());
					logScheduledTasks();
				},
				() -> log.warn("Failed to acquire lock for Schedule ID: {}", target.scheduleId())
			);
	}

	private void cancelScheduledTaskById(Long scheduleId) {
		ScheduledFuture<?> scheduledTask = scheduledTasks.get(scheduleId);
		if (scheduledTask != null && !scheduledTask.isDone()) {
			scheduledTask.cancel(true);
		}
		scheduledTasks.remove(scheduleId);
	}

	// 현재 등록된 스케줄 로그 출력
	public void logScheduledTasks() {
		scheduledTasks.forEach((scheduleId, future) ->
			log.debug("Scheduled task for Schedule ID: {} is currently {}.",
				scheduleId, (future.isCancelled() ? "Cancelled" : "Scheduled"))
		);
	}
}
