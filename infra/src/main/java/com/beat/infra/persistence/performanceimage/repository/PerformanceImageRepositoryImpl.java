package com.beat.infra.persistence.performanceimage.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.beat.domain.performance.domain.PerformanceImage;
import com.beat.domain.performance.repository.PerformanceImageRepository;
import com.beat.infra.persistence.performanceimage.entity.PerformanceImageJpaEntity;
import com.beat.infra.persistence.performanceimage.mapper.PerformanceImagePersistenceMapper;

@Repository
public class PerformanceImageRepositoryImpl implements PerformanceImageRepository {

	private final PerformanceImageJpaRepository performanceImageJpaRepository;
	private final PerformanceImagePersistenceMapper performanceImagePersistenceMapper;

	public PerformanceImageRepositoryImpl(
		PerformanceImageJpaRepository performanceImageJpaRepository,
		PerformanceImagePersistenceMapper performanceImagePersistenceMapper
	) {
		this.performanceImageJpaRepository = performanceImageJpaRepository;
		this.performanceImagePersistenceMapper = performanceImagePersistenceMapper;
	}

	@Override
	public Optional<PerformanceImage> findById(Long id) {
		return performanceImageJpaRepository.findById(id)
			.map(performanceImagePersistenceMapper::toDomain);
	}

	@Override
	public List<PerformanceImage> findAllByPerformanceId(Long performanceId) {
		return performanceImageJpaRepository.findAllByPerformanceId(performanceId).stream()
			.map(performanceImagePersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public List<Long> findIdsByPerformanceId(Long performanceId) {
		return performanceImageJpaRepository.findIdsByPerformanceId(performanceId);
	}

	@Override
	public PerformanceImage save(PerformanceImage performanceImage) {
		PerformanceImageJpaEntity saved = performanceImageJpaRepository.save(
			performanceImagePersistenceMapper.toEntity(performanceImage)
		);
		return performanceImagePersistenceMapper.toDomain(saved);
	}

	@Override
	public List<PerformanceImage> saveAll(List<PerformanceImage> performanceImages) {
		List<PerformanceImageJpaEntity> entities = performanceImages.stream()
			.map(performanceImagePersistenceMapper::toEntity)
			.toList();
		return performanceImageJpaRepository.saveAll(entities).stream()
			.map(performanceImagePersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public void delete(PerformanceImage performanceImage) {
		if (performanceImage.getId() == null) {
			throw new IllegalArgumentException("Cannot delete unpersisted PerformanceImage");
		}
		performanceImageJpaRepository.deleteById(performanceImage.getId());
	}

	@Override
	public void deleteByPerformanceId(Long performanceId) {
		performanceImageJpaRepository.deleteByPerformanceId(performanceId);
	}
}
