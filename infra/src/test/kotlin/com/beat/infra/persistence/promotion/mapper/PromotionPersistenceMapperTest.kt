package com.beat.infra.persistence.promotion.mapper

import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.model.Promotion
import com.beat.infra.persistence.promotion.entity.PromotionJpaEntity
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromotionPersistenceMapperTest {

    private val mapper = PromotionPersistenceMapper()

    @Test
    fun toDomainPreservesJpaEntityFieldsUsedByJavaCallers() {
        val entity = PromotionJpaEntity.rehydrate(
            11L,
            "https://example.com/promotion.png",
            22L,
            "https://example.com/performance",
            true,
            CarouselNumber.THREE,
        )

        val promotion = mapper.toDomain(entity)

        assertAll(
            { assertEquals(11L, promotion.getId()) },
            { assertEquals("https://example.com/promotion.png", promotion.promotionPhoto) },
            { assertEquals(22L, promotion.getPerformanceId()) },
            { assertEquals("https://example.com/performance", promotion.redirectUrl) },
            { assertTrue(promotion.isExternal) },
            { assertEquals(CarouselNumber.THREE, promotion.carouselNumber) },
        )
    }

    @Test
    fun toEntityKeepsGeneratedIdNullForNewPromotion() {
        val promotion = Promotion.create(
            "https://example.com/new.png",
            44L,
            "https://example.com/new-performance",
            true,
            CarouselNumber.TWO,
        )

        val entity = mapper.toEntity(promotion)

        assertAll(
            { assertNull(promotion.getId()) },
            { assertNull(entity.id) },
            { assertEquals(44L, entity.performanceId) },
            { assertEquals(CarouselNumber.TWO, entity.carouselNumber) },
        )
    }

    @Test
    fun toEntityPreservesDomainFieldsAndJavaVisiblePromotionJpaEntityContract() {
        val promotion = Promotion.rehydrate(
            31L,
            "https://example.com/internal.png",
            null,
            "/notices/31",
            false,
            CarouselNumber.ONE,
        )

        val entity = mapper.toEntity(promotion)

        assertAll(
            { assertEquals(31L, entity.id) },
            { assertEquals("https://example.com/internal.png", entity.promotionPhoto) },
            { assertNull(entity.performanceId) },
            { assertEquals("/notices/31", entity.redirectUrl) },
            { assertFalse(entity.isExternal) },
            { assertEquals(CarouselNumber.ONE, entity.carouselNumber) },
        )
    }
}
