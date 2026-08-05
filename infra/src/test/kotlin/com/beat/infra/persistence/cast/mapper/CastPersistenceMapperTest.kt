package com.beat.infra.persistence.cast.mapper

import com.beat.domain.performance.model.Cast
import com.beat.infra.persistence.cast.entity.CastJpaEntity
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CastPersistenceMapperTest {
    private val mapper = CastPersistenceMapper()

    @Test
    fun toDomainPreservesJpaEntityFieldsIncludingScalarPerformanceId() {
        val entity = CastJpaEntity.rehydrate(
            11L,
            "cast-name",
            "cast-role",
            "https://example.com/cast.png",
            22L,
        )

        val cast = mapper.toDomain(entity)

        assertAll(
            { assertEquals(11L, cast.getId()) },
            { assertEquals("cast-name", cast.castName) },
            { assertEquals("cast-role", cast.castRole) },
            { assertEquals("https://example.com/cast.png", cast.castPhoto) },
        )
    }

    @Test
    fun toEntityKeepsGeneratedIdNullForNewCast() {
        val cast = Cast.create(
            "new-cast",
            "new-role",
            "https://example.com/new-cast.png",
        )

        val entity = mapper.toEntity(cast, 44L)

        assertAll(
            { assertNull(cast.getId()) },
            { assertNull(entity.id) },
            { assertEquals("new-cast", entity.castName) },
            { assertEquals("new-role", entity.castRole) },
            { assertEquals("https://example.com/new-cast.png", entity.castPhoto) },
            { assertEquals(44L, entity.performanceId) },
        )
    }

    @Test
    fun toEntityPreservesRehydratedDomainFields() {
        val cast = Cast.rehydrate(
            31L,
            "existing-cast",
            "existing-role",
            "https://example.com/existing-cast.png",
        )

        val entity = mapper.toEntity(cast, 41L)

        assertAll(
            { assertEquals(31L, entity.id) },
            { assertEquals("existing-cast", entity.castName) },
            { assertEquals("existing-role", entity.castRole) },
            { assertEquals("https://example.com/existing-cast.png", entity.castPhoto) },
            { assertEquals(41L, entity.performanceId) },
        )
    }
}
