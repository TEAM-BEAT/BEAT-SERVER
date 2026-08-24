package com.beat.infrastructure.persistence.staff.mapper

import com.beat.domain.performance.model.Staff
import com.beat.infrastructure.persistence.staff.entity.StaffJpaEntity
import org.springframework.stereotype.Component

@Component
internal class StaffPersistenceMapper {
    fun toDomain(entity: StaffJpaEntity): Staff = Staff.rehydrate(
        entity.id,
        entity.staffName,
        entity.staffRole,
        entity.staffPhoto,
    )

    fun toEntity(staff: Staff, performanceId: Long): StaffJpaEntity = StaffJpaEntity.rehydrate(
        staff.id,
        staff.staffName,
        staff.staffRole,
        staff.staffPhoto,
        performanceId,
    )
}
