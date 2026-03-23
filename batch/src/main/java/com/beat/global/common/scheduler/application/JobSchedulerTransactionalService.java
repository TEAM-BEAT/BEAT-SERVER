package com.beat.global.common.scheduler.application;

import java.util.List;
import java.util.Optional;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobSchedulerTransactionalService {

	private final ScheduleRepository scheduleRepository;

	@Transactional(readOnly = true)
	public List<Schedule> findPendingSchedules() {
		return scheduleRepository.findPendingSchedulesWithPerformance();
	}

	@Transactional
	public Optional<Schedule> lockSchedule(Long scheduleId) {
		return scheduleRepository.lockById(scheduleId)
			.map(schedule -> {
				Hibernate.initialize(schedule.getPerformance());
				return schedule;
			});
	}

	@Transactional
	public void closeBooking(Long scheduleId) {
		Schedule schedule = scheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new IllegalStateException("Schedule not found: " + scheduleId));

		schedule.updateIsBooking(false);
	}
}
