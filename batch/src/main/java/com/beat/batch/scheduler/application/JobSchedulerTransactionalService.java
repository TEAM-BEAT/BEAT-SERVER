package com.beat.batch.scheduler.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.batch.scheduler.application.result.LockedScheduleBookingWindow;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobSchedulerTransactionalService {

	private final ScheduleRepository scheduleRepository;

	@Transactional(readOnly = true)
	public List<Long> findPendingScheduleIds() {
		return scheduleRepository.findPendingSchedules()
			.stream()
			.map(Schedule::getId)
			.toList();
	}

	@Transactional
	public Optional<LockedScheduleBookingWindow> lockScheduleBookingWindow(Long scheduleId) {
		return scheduleRepository.lockById(scheduleId)
			.map(this::toLockedScheduleBookingWindow);
	}

	@Transactional
	public void closeBooking(Long scheduleId) {
		Schedule schedule = scheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new IllegalStateException("Schedule not found: " + scheduleId));

		scheduleRepository.save(schedule.updateIsBooking(false));
	}

	private LockedScheduleBookingWindow toLockedScheduleBookingWindow(Schedule schedule) {
		return LockedScheduleBookingWindow.of(
			schedule.getId(),
			schedule.getPerformanceId(),
			schedule.getPerformanceDate()
		);
	}
}
