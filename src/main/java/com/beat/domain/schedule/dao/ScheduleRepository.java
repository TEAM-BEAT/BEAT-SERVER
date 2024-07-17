package com.beat.domain.schedule.dao;

import com.beat.domain.schedule.domain.Schedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Schedule s WHERE s.id = :id")
    Optional<Schedule> lockById(@Param("id") Long id);

    List<Schedule> findByPerformanceId(Long performanceId);

    List<Schedule> findAllByPerformanceId(Long performanceId);

}