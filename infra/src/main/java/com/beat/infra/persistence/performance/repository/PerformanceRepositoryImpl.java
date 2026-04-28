package com.beat.infra.persistence.performance.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.infra.persistence.performance.entity.PerformanceJpaEntity;
import com.beat.infra.persistence.performance.mapper.PerformancePersistenceMapper;

@Repository
public class PerformanceRepositoryImpl implements PerformanceRepository {

	private final PerformanceJpaRepository performanceJpaRepository;
	private final PerformancePersistenceMapper performancePersistenceMapper;

	public PerformanceRepositoryImpl(PerformanceJpaRepository performanceJpaRepository,
		PerformancePersistenceMapper performancePersistenceMapper) {
		this.performanceJpaRepository = performanceJpaRepository;
		this.performancePersistenceMapper = performancePersistenceMapper;
	}

	@Override
	public Optional<Performance> findById(Long id) {
		return performanceJpaRepository.findById(id).map(performancePersistenceMapper::toDomain);
	}

	@Override
	public List<Performance> findAll() {
		return performanceJpaRepository.findAll().stream()
			.map(performancePersistenceMapper::toDomain)
			.collect(Collectors.toList());
	}

	@Override
	public Performance save(Performance performance) {
		PerformanceJpaEntity entity = performancePersistenceMapper.toEntity(performance);
		PerformanceJpaEntity savedEntity = performanceJpaRepository.save(entity);
		return performancePersistenceMapper.toDomain(savedEntity);
	}

	@Override
	public List<Performance> findByGenre(Genre genre) {
		return performanceJpaRepository.findByGenre(genre).stream()
			.map(performancePersistenceMapper::toDomain)
			.collect(Collectors.toList());
	}

	@Override
	public List<Performance> findByUserId(Long userId) {
		return performanceJpaRepository.findByUserId(userId).stream()
			.map(performancePersistenceMapper::toDomain)
			.collect(Collectors.toList());
	}

	@Override
	public void deleteById(Long id) {
		performanceJpaRepository.deleteById(id);
	}
}
