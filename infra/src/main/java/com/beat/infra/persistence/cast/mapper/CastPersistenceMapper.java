package com.beat.infra.persistence.cast.mapper;

import org.springframework.stereotype.Component;

import com.beat.domain.performance.model.Cast;
import com.beat.infra.persistence.cast.entity.CastJpaEntity;

@Component
public class CastPersistenceMapper {

	public Cast toDomain(CastJpaEntity entity) {
		return Cast.rehydrate(
			entity.getId(),
			entity.getCastName(),
			entity.getCastRole(),
			entity.getCastPhoto()
		);
	}

	public CastJpaEntity toEntity(Cast cast, long performanceId) {
		return CastJpaEntity.rehydrate(
			cast.getId(),
			cast.getCastName(),
			cast.getCastRole(),
			cast.getCastPhoto(),
			performanceId
		);
	}
}
