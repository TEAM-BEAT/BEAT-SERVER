package com.beat.domain.performance.repository;

import java.util.List;
import java.util.Optional;

import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.domain.Performance;

public interface PerformanceRepository {
	Optional<Performance> findById(Long id);

	List<Performance> findAll();

	Performance save(Performance performance);

	List<Performance> findByGenre(Genre genre);

	List<Performance> findByUserId(Long userId);

	void deleteById(Long id);
}
