package com.beat.infra.persistence.cast.mapper;

import org.springframework.stereotype.Component;

import com.beat.domain.cast.domain.Cast;
import com.beat.infra.persistence.cast.entity.CastJpaEntity;

@Component
public class CastPersistenceMapper {

	public Cast toDomain(CastJpaEntity entity) {
		return Cast.rehydrate(
			entity.getId(),
			entity.getCastName(),
			entity.getCastRole(),
			entity.getCastPhoto(),
			entity.getPerformanceId()
		);
	}

	public CastJpaEntity toEntity(Cast cast) {
		return CastJpaEntity.rehydrate(
			cast.getId(),
			cast.getCastName(),
			cast.getCastRole(),
			cast.getCastPhoto(),
			cast.getPerformanceId()
		);
	}
}
