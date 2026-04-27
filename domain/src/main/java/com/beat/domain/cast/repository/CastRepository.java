package com.beat.domain.cast.repository;

import java.util.List;
import java.util.Optional;

import com.beat.domain.cast.domain.Cast;

public interface CastRepository {

	Optional<Cast> findById(Long castId);

	List<Cast> findAllByPerformanceId(Long performanceId);

	List<Long> findIdsByPerformanceId(Long performanceId);

	Cast save(Cast cast);

	List<Cast> saveAll(List<Cast> casts);

	void delete(Cast cast);

	void deleteByPerformanceId(Long performanceId);
}
