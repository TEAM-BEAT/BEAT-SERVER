package com.beat.infra.persistence.promotion.mapper;

import org.springframework.stereotype.Component;

import com.beat.domain.promotion.domain.Promotion;
import com.beat.infra.persistence.promotion.entity.PromotionJpaEntity;

@Component
public class PromotionPersistenceMapper {

	public Promotion toDomain(PromotionJpaEntity entity) {
		return Promotion.rehydrate(
			entity.getId(),
			entity.getPromotionPhoto(),
			entity.getPerformanceId(),
			entity.getRedirectUrl(),
			entity.isExternal(),
			entity.getCarouselNumber()
		);
	}

	public PromotionJpaEntity toEntity(Promotion promotion) {
		return PromotionJpaEntity.rehydrate(
			promotion.getId(),
			promotion.getPromotionPhoto(),
			promotion.getPerformanceId(),
			promotion.getRedirectUrl(),
			promotion.isExternal(),
			promotion.getCarouselNumber()
		);
	}
}
