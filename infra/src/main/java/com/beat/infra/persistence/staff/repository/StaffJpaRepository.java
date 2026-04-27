package com.beat.infra.persistence.staff.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beat.infra.persistence.staff.entity.StaffJpaEntity;

public interface StaffJpaRepository extends JpaRepository<StaffJpaEntity, Long> {

	List<StaffJpaEntity> findByPerformanceId(Long performanceId);

	List<StaffJpaEntity> findAllByPerformanceId(Long performanceId);

	@Query("SELECT s.id FROM Staff s WHERE s.performanceId = :performanceId")
	List<Long> findIdsByPerformanceId(@Param("performanceId") Long performanceId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM Staff s WHERE s.performanceId = :performanceId")
	void deleteByPerformanceId(@Param("performanceId") Long performanceId);
}
