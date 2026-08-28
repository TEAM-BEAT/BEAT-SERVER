package com.beat.infrastructure.persistence.performanceimage.mapper

import com.beat.domain.performance.model.PerformanceImage
import com.beat.infrastructure.persistence.performanceimage.entity.PerformanceImageJpaEntity
import org.springframework.stereotype.Component

@Component
internal class PerformanceImagePersistenceMapper {
    fun toDomain(entity: PerformanceImageJpaEntity): PerformanceImage =
        PerformanceImage.rehydrate(
            entity.id,
            entity.performanceImageUrl,
        )

    fun toEntity(
        performanceImage: PerformanceImage,
        performanceId: Long,
    ): PerformanceImageJpaEntity =
        PerformanceImageJpaEntity.rehydrate(
            performanceImage.id,
            performanceImage.performanceImageUrl,
            performanceId,
        )
}
