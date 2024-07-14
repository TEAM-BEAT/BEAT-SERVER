package com.beat.domain.performance.dao;

import com.beat.domain.performance.domain.Performance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {
}