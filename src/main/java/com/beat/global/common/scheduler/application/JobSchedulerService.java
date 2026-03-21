package com.beat.global.common.scheduler.application;

import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSchedulerService {

	private final JobSchedulerTransactionalService jobSchedulerTransactionalService;
	private final TaskScheduler taskScheduler;

	@Value("${beat.scheduler.owner:false}")
	private boolean schedulerOwner;

	// 스케줄 ID와 관련된 작업을 관리하기 위한 ConcurrentHashMap 선언
	private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

	// ApplicationReadyEvent는 애플리케이션이 완전히 시작된 후에 한 번만 실행됩니다.
	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady(ApplicationReadyEvent event) {
		if (!schedulerOwner) {
			log.info("Skipping schedule rehydration because this runtime is not the scheduler owner.");
			return;
		}

		log.info("onApplicationReady() method triggered.");
		reconcilePendingSchedules();
	}

	@Scheduled(fixedDelayString = "${beat.scheduler.reconcile-interval-ms:60000}")
	public void reconcilePendingSchedules() {
		if (!schedulerOwner) {
			return;
		}

		List<Schedule> schedules = jobSchedulerTransactionalService.findPendingSchedules();
		List<Long> pendingScheduleIds = new ArrayList<>();

		schedules.forEach(schedule -> {
			pendingScheduleIds.add(schedule.getId());
			addScheduleIfNotExists(schedule);
		});

		new ArrayList<>(scheduledTasks.keySet()).stream()
			.filter(scheduleId -> !pendingScheduleIds.contains(scheduleId))
			.forEach(this::cancelScheduledTaskById);
	}

	public void schedulePendingPerformances() {
		reconcilePendingSchedules();
	}

	// 스케줄 종료 시점에 맞춰 isBooking 업데이트
	public void addScheduleIfNotExists(Schedule schedule) {
		if (!schedulerOwner) {
			log.info("Ignoring schedule registration because this runtime is not the scheduler owner.");
			return;
		}

		ScheduledFuture<?> existingTask = scheduledTasks.get(schedule.getId());
		if (existingTask != null) {
			if (!existingTask.isDone() && !existingTask.isCancelled()) {
				log.debug("Schedule ID {} is already scheduled. Skipping duplicate registration.", schedule.getId());
				return;
			}
			scheduledTasks.remove(schedule.getId());
		}

		schedulePendingTask(schedule);
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

	public void cancelScheduledTaskForPerformance(Schedule schedule) {
		if (!schedulerOwner) {
			log.info("Ignoring schedule cancellation because this runtime is not the scheduler owner.");
			return;
		}

		cancelScheduledTaskById(schedule.getId());
	}

	private void schedulePendingTask(Schedule schedule) {
		// 여기서 데이터베이스 X-Lock을 걸어 중복 실행 방지
		jobSchedulerTransactionalService.lockSchedule(schedule.getId())
			.ifPresentOrElse(
				lockedSchedule -> {
					log.info("Lock acquired for Schedule ID: {}", lockedSchedule.getId());
					LocalDateTime performanceEndTime = lockedSchedule.getPerformanceDate()
						.plusMinutes(lockedSchedule.getPerformance().getRunningTime());

					log.info("Scheduling task for Schedule ID: {} at {}", lockedSchedule.getId(), performanceEndTime);

					ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
						() -> updateIsBookingFalse(lockedSchedule.getId()),
						Date.from(performanceEndTime.atZone(ZoneId.systemDefault()).toInstant())
					);

					scheduledTasks.put(lockedSchedule.getId(), scheduledTask);
					log.debug("Task added for Schedule ID: {}", lockedSchedule.getId());
					logScheduledTasks();
				},
				() -> log.warn("Failed to acquire lock for Schedule ID: {}", schedule.getId())
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
		scheduledTasks.forEach((scheduleId, future) -> {
			log.debug("Scheduled task for Schedule ID: {} is currently {}.",
				scheduleId, (future.isCancelled() ? "Cancelled" : "Scheduled"));
		});
	}
}
