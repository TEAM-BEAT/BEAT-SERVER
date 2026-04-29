package com.beat.domain.schedule.repository;

import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.repository.dto.MinPerformanceDateDto;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {
    Optional<Schedule> findById(Long id);

    Optional<Schedule> lockById(Long id);

    List<Schedule> findAllByPerformanceId(Long performanceId);

    List<Long> findIdsByPerformanceId(Long performanceId);

    int countByPerformanceId(Long performanceId);

    Schedule save(Schedule schedule);

    List<Schedule> saveAll(List<Schedule> schedules);

    void delete(Schedule schedule);

    void deleteByPerformanceId(Long performanceId);

    List<Schedule> findPendingSchedules();

    List<MinPerformanceDateDto> findMinPerformanceDateByPerformanceIds(List<Long> performanceIds);
}
