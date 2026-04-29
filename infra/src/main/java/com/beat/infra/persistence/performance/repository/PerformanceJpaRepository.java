package com.beat.infra.persistence.performance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beat.domain.performance.domain.Genre;
import com.beat.infra.persistence.performance.entity.PerformanceJpaEntity;

public interface PerformanceJpaRepository extends JpaRepository<PerformanceJpaEntity, Long> {
	List<PerformanceJpaEntity> findByGenre(Genre genre);

	List<PerformanceJpaEntity> findByUserId(Long userId);
}
