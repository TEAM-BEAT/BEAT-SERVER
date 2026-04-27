package com.beat.infra.persistence.cast.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.beat.domain.cast.domain.Cast;
import com.beat.domain.cast.repository.CastRepository;
import com.beat.infra.persistence.cast.entity.CastJpaEntity;
import com.beat.infra.persistence.cast.mapper.CastPersistenceMapper;

@Repository
public class CastRepositoryImpl implements CastRepository {

	private final CastJpaRepository castJpaRepository;
	private final CastPersistenceMapper castPersistenceMapper;

	public CastRepositoryImpl(CastJpaRepository castJpaRepository, CastPersistenceMapper castPersistenceMapper) {
		this.castJpaRepository = castJpaRepository;
		this.castPersistenceMapper = castPersistenceMapper;
	}

	@Override
	public Optional<Cast> findById(Long castId) {
		return castJpaRepository.findById(castId)
			.map(castPersistenceMapper::toDomain);
	}

	@Override
	public List<Cast> findByPerformanceId(Long performanceId) {
		return castJpaRepository.findByPerformanceId(performanceId).stream()
			.map(castPersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public List<Cast> findAllByPerformanceId(Long performanceId) {
		return castJpaRepository.findAllByPerformanceId(performanceId).stream()
			.map(castPersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public List<Long> findIdsByPerformanceId(Long performanceId) {
		return castJpaRepository.findIdsByPerformanceId(performanceId);
	}

	@Override
	public Cast save(Cast cast) {
		CastJpaEntity savedCast = castJpaRepository.save(castPersistenceMapper.toEntity(cast));
		return castPersistenceMapper.toDomain(savedCast);
	}

	@Override
	public List<Cast> saveAll(List<Cast> casts) {
		List<CastJpaEntity> castEntities = casts.stream()
			.map(castPersistenceMapper::toEntity)
			.toList();
		return castJpaRepository.saveAll(castEntities).stream()
			.map(castPersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public void delete(Cast cast) {
		if (cast.getId() != null) {
			castJpaRepository.deleteById(cast.getId());
		}
	}

	@Override
	public void deleteByPerformanceId(Long performanceId) {
		castJpaRepository.deleteByPerformanceId(performanceId);
	}
}
