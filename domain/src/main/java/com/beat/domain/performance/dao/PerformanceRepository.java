package com.beat.domain.performance.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.domain.Performance;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {
	List<Performance> findByGenre(Genre genre);

	List<Performance> findByUserId(Long userId);

}
