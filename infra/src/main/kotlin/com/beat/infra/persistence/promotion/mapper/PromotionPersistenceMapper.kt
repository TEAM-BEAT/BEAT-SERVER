package com.beat.infra.persistence.promotion.mapper

import com.beat.domain.promotion.model.Promotion
import com.beat.infra.persistence.promotion.entity.PromotionJpaEntity
import org.springframework.stereotype.Component

@Component
class PromotionPersistenceMapper {

    fun toDomain(entity: PromotionJpaEntity): Promotion =
        Promotion.rehydrate(
            entity.id,
            entity.promotionPhoto,
            entity.performanceId,
            entity.redirectUrl,
            entity.isExternal,
            entity.carouselNumber,
        )

    fun toEntity(promotion: Promotion): PromotionJpaEntity =
        PromotionJpaEntity.rehydrate(
            promotion.getId(),
            promotion.promotionPhoto,
            promotion.getPerformanceId(),
            promotion.redirectUrl,
            promotion.isExternal,
            promotion.carouselNumber,
        )
}
