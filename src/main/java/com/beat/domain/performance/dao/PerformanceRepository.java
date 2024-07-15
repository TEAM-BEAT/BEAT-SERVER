package com.beat.domain.performance.dao;

import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.domain.Performance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    List<Performance> findByGenre(Genre genre);
    List<Performance> findByUsersId(Long userId);

}