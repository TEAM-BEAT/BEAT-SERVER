package com.beat.infra.persistence.performanceimage.mapper;

import org.springframework.stereotype.Component;

import com.beat.domain.performanceimage.domain.PerformanceImage;
import com.beat.infra.persistence.performanceimage.entity.PerformanceImageJpaEntity;

@Component
public class PerformanceImagePersistenceMapper {

	public PerformanceImage toDomain(PerformanceImageJpaEntity entity) {
		return PerformanceImage.rehydrate(
			entity.getId(),
			entity.getPerformanceImageUrl(),
			entity.getPerformanceId()
		);
	}

	public PerformanceImageJpaEntity toEntity(PerformanceImage performanceImage) {
		return PerformanceImageJpaEntity.rehydrate(
			performanceImage.getId(),
			performanceImage.getPerformanceImageUrl(),
			performanceImage.getPerformanceId()
		);
	}
}
