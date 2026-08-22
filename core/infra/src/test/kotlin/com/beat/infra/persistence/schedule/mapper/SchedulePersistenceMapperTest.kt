package com.beat.infra.persistence.schedule.mapper

import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.infra.persistence.exception.PersistenceMappingException
import com.beat.infra.persistence.schedule.entity.ScheduleJpaEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import java.time.LocalDateTime

class SchedulePersistenceMapperTest : FunSpec({
    val mapper = SchedulePersistenceMapper()

    test("invalidStoredStateIsNotExposedAsAClientDomainError") {
        val performanceDate = LocalDateTime.of(2026, 7, 17, 20, 0)
        val corruptedRow = ScheduleJpaEntity.rehydrate(
            1L,
            performanceDate,
            performanceDate.minusMinutes(1),
            10,
            0,
            ScheduleNumber.FIRST,
            2L,
        )

        shouldThrow<PersistenceMappingException> { mapper.toDomain(corruptedRow) }
    }
})
