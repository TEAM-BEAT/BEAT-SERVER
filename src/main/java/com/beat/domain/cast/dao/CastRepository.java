package com.beat.domain.cast.dao;

import com.beat.domain.cast.domain.Cast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CastRepository extends JpaRepository<Cast, Long> {
    List<Cast> findByPerformanceId(Long performanceId);

    List<Cast> findAllByPerformanceId(Long performanceId);

    @Query("SELECT c.id FROM Cast c WHERE c.performance.id = :performanceId")
    List<Long> findIdsByPerformanceId(Long performanceId);
}
