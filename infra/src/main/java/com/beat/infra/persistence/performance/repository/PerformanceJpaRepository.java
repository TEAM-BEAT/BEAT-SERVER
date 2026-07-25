package com.beat.infra.persistence.performance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beat.infra.persistence.performance.entity.PerformanceJpaEntity;

import jakarta.persistence.LockModeType;

public interface PerformanceJpaRepository extends JpaRepository<PerformanceJpaEntity, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Performance p WHERE p.id = :id")
	Optional<PerformanceJpaEntity> lockById(@Param("id") Long id);
}
