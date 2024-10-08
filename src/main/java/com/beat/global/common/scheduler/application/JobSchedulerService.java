package com.beat.global.common.scheduler.application;

import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSchedulerService {

    private final ScheduleRepository scheduleRepository;
    private final TaskScheduler taskScheduler;

    // 스케줄 ID와 관련된 작업을 관리하기 위한 ConcurrentHashMap 선언
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    // ApplicationReadyEvent는 애플리케이션이 완전히 시작된 후에 한 번만 실행됩니다.
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady(ApplicationReadyEvent event) {
        // 서버 시작 시점에만 실행되도록 트랜잭션 관리가 필요한 부분을 다른 메서드로 분리
        log.info("onApplicationReady() method triggered.");
        schedulePendingPerformances();
    }

    @Transactional(readOnly = true)
    public void schedulePendingPerformances() {
        // PENDING 상태의 스케줄들을 조회하여 트랜잭션 내에서 처리
        List<Schedule> schedules = scheduleRepository.findPendingSchedules();

        // 각각의 스케줄에 대해 지연 로딩 처리 및 스케줄링 작업 수행
        schedules.forEach(schedule -> {
            // Performance 초기화 시점은 트랜잭션 경계 내에서 이루어져야 함
            Hibernate.initialize(schedule.getPerformance());
            addScheduleIfNotExists(schedule);
        });
    }

    // 스케줄 종료 시점에 맞춰 isBooking 업데이트
    @Transactional
    public void addScheduleIfNotExists(Schedule schedule) {
        if (scheduledTasks.containsKey(schedule.getId())) {
            log.debug("Schedule ID {} is already scheduled. Skipping duplicate registration.", schedule.getId());
            return;
        }

        // 여기서 데이터베이스 X-Lock을 걸어 중복 실행 방지
        scheduleRepository.lockById(schedule.getId())
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

    // 스케줄 종료 시 isBooking을 false로 업데이트
    @Transactional
    public void updateIsBookingFalse(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalStateException("Schedule not found: " + scheduleId));

        log.info("Updating isBooking to false for schedule ID: {}", scheduleId);
        schedule.updateIsBooking(false);
        scheduleRepository.save(schedule);

        // 스케줄 작업 완료 후 Map에서 삭제
        scheduledTasks.remove(scheduleId);
        log.debug("Completed Task removed for Schedule ID: {}", scheduleId);
        logScheduledTasks();
    }

    public void cancelScheduledTaskForPerformance(Schedule schedule) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(schedule.getId());
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(true);  // 작업이 완료되지 않았다면 취소
            scheduledTasks.remove(schedule.getId());
            log.info("Cancelled Task removed for Schedule ID: {}", schedule.getId());
            logScheduledTasks();
        }
    }

    // 현재 등록된 스케줄 로그 출력
    public void logScheduledTasks() {
        scheduledTasks.forEach((scheduleId, future) -> {
            log.debug("Scheduled task for Schedule ID: {} is currently {}.",
                    scheduleId, (future.isCancelled() ? "Cancelled" : "Scheduled"));
        });
    }
}
