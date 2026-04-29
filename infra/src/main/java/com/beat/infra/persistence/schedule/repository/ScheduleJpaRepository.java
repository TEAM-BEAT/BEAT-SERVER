package com.beat.infra.persistence.schedule.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beat.infra.persistence.schedule.entity.ScheduleJpaEntity;

import jakarta.persistence.LockModeType;

public interface ScheduleJpaRepository extends JpaRepository<ScheduleJpaEntity, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM Schedule s WHERE s.id = :id")
	Optional<ScheduleJpaEntity> lockById(@Param("id") Long id);

	List<ScheduleJpaEntity> findAllByPerformanceId(Long performanceId);

	@Query("SELECT s.id FROM Schedule s WHERE s.performanceId = :performanceId")
	List<Long> findIdsByPerformanceId(@Param("performanceId") Long performanceId);

	int countByPerformanceId(Long performanceId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM Schedule s WHERE s.performanceId = :performanceId")
	void deleteByPerformanceId(@Param("performanceId") Long performanceId);

	@Query("SELECT s FROM Schedule s WHERE s.isBooking = true")
	List<ScheduleJpaEntity> findPendingSchedules();
}
