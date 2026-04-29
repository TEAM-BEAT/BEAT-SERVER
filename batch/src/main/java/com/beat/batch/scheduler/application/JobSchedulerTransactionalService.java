package com.beat.batch.scheduler.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobSchedulerTransactionalService {

	private final ScheduleRepository scheduleRepository;

	@Transactional(readOnly = true)
	public List<Schedule> findPendingSchedules() {
		return scheduleRepository.findPendingSchedules();
	}

	@Transactional
	public Optional<Schedule> lockSchedule(Long scheduleId) {
		return scheduleRepository.lockById(scheduleId);
	}

	@Transactional
	public void closeBooking(Long scheduleId) {
		Schedule schedule = scheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new IllegalStateException("Schedule not found: " + scheduleId));

		scheduleRepository.save(schedule.updateIsBooking(false));
	}
}
