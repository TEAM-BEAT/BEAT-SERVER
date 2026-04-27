package com.beat.infra.persistence.cast.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beat.infra.persistence.cast.entity.CastJpaEntity;

public interface CastJpaRepository extends JpaRepository<CastJpaEntity, Long> {

	List<CastJpaEntity> findByPerformanceId(Long performanceId);

	List<CastJpaEntity> findAllByPerformanceId(Long performanceId);

	@Query("SELECT c.id FROM Cast c WHERE c.performanceId = :performanceId")
	List<Long> findIdsByPerformanceId(@Param("performanceId") Long performanceId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM Cast c WHERE c.performanceId = :performanceId")
	void deleteByPerformanceId(@Param("performanceId") Long performanceId);
}
