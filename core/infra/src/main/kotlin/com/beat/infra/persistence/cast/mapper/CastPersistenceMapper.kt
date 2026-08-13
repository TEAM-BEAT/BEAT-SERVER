package com.beat.infra.persistence.cast.mapper

import com.beat.domain.performance.model.Cast
import com.beat.infra.persistence.cast.entity.CastJpaEntity
import org.springframework.stereotype.Component

@Component
class CastPersistenceMapper {
    fun toDomain(entity: CastJpaEntity): Cast = Cast.rehydrate(
        entity.id,
        entity.castName,
        entity.castRole,
        entity.castPhoto,
    )

    fun toEntity(cast: Cast, performanceId: Long): CastJpaEntity = CastJpaEntity.rehydrate(
        cast.getId(),
        cast.castName,
        cast.castRole,
        cast.castPhoto,
        performanceId,
    )
}
