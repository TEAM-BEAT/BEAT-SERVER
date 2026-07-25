package com.beat.infra.persistence.performanceimage.mapper;

import org.springframework.stereotype.Component;

import com.beat.domain.performance.model.PerformanceImage;
import com.beat.infra.persistence.performanceimage.entity.PerformanceImageJpaEntity;

@Component
public class PerformanceImagePersistenceMapper {

	public PerformanceImage toDomain(PerformanceImageJpaEntity entity) {
		return PerformanceImage.rehydrate(
			entity.getId(),
			entity.getPerformanceImageUrl()
		);
	}

	public PerformanceImageJpaEntity toEntity(PerformanceImage performanceImage, long performanceId) {
		return PerformanceImageJpaEntity.rehydrate(
			performanceImage.getId(),
			performanceImage.getPerformanceImageUrl(),
			performanceId
		);
	}
}
