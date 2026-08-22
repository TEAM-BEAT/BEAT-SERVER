package com.beat.infra.persistence.performanceimage.mapper

import com.beat.domain.performance.model.PerformanceImage
import com.beat.infra.persistence.performanceimage.entity.PerformanceImageJpaEntity
import org.springframework.stereotype.Component

@Component
internal class PerformanceImagePersistenceMapper {
    fun toDomain(entity: PerformanceImageJpaEntity): PerformanceImage = PerformanceImage.rehydrate(
        entity.id,
        entity.performanceImageUrl,
    )

    fun toEntity(performanceImage: PerformanceImage, performanceId: Long): PerformanceImageJpaEntity =
        PerformanceImageJpaEntity.rehydrate(
            performanceImage.id,
            performanceImage.performanceImageUrl,
            performanceId,
        )
}
