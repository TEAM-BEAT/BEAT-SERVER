package com.beat.domain.performance.dao;

import com.beat.domain.performance.domain.PerformanceImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceImageRepository extends JpaRepository<PerformanceImage, Long> {
    List<PerformanceImage> findAllByPerformanceId(Long performanceId);
}
