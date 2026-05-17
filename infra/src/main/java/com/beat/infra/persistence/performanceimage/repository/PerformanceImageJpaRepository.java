package com.beat.infra.persistence.performanceimage.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beat.infra.persistence.performanceimage.entity.PerformanceImageJpaEntity;

public interface PerformanceImageJpaRepository extends JpaRepository<PerformanceImageJpaEntity, Long> {

	List<PerformanceImageJpaEntity> findAllByPerformanceId(Long performanceId);

	@Query("SELECT p.id FROM PerformanceImage p WHERE p.performanceId = :performanceId")
	List<Long> findIdsByPerformanceId(@Param("performanceId") Long performanceId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM PerformanceImage p WHERE p.performanceId = :performanceId")
	void deleteByPerformanceId(@Param("performanceId") Long performanceId);
}
