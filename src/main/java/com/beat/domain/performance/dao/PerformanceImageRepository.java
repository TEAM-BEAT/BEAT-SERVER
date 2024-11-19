package com.beat.domain.performance.dao;

import com.beat.domain.performance.domain.PerformanceImage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PerformanceImageRepository extends JpaRepository<PerformanceImage, Long> {
	List<PerformanceImage> findAllByPerformanceId(Long performanceId);

	@Query("SELECT s.id FROM PerformanceImage s WHERE s.performance.id = :performanceId")
	List<Long> findIdsByPerformanceId(Long performanceId);
}
