package com.beat.infra.persistence.promotion.mapper

import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.model.Promotion
import com.beat.infra.persistence.promotion.entity.PromotionJpaEntity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PromotionPersistenceMapperTest : FunSpec({
    val mapper = PromotionPersistenceMapper()

    test("toDomainPreservesJpaEntityFields") {
        val entity = PromotionJpaEntity.rehydrate(
            11L,
            "https://example.com/promotion.png",
            22L,
            "https://example.com/performance",
            true,
            CarouselNumber.THREE,
        )

        val promotion = mapper.toDomain(entity)

        promotion.id shouldBe 11L
        promotion.promotionPhoto shouldBe "https://example.com/promotion.png"
        promotion.performanceId shouldBe 22L
        promotion.redirectUrl shouldBe "https://example.com/performance"
        promotion.isExternal shouldBe true
        promotion.carouselNumber shouldBe CarouselNumber.THREE
    }

    test("toEntityKeepsGeneratedIdNullForNewPromotion") {
        val promotion = Promotion.create(
            "https://example.com/new.png",
            44L,
            "https://example.com/new-performance",
            true,
            CarouselNumber.TWO,
        )

        val entity = mapper.toEntity(promotion)

        promotion.id shouldBe null
        entity.id shouldBe null
        entity.performanceId shouldBe 44L
        entity.carouselNumber shouldBe CarouselNumber.TWO
    }

    test("toEntityPreservesDomainFields") {
        val promotion = Promotion.rehydrate(
            31L,
            "https://example.com/internal.png",
            null,
            "/notices/31",
            false,
            CarouselNumber.ONE,
        )

        val entity = mapper.toEntity(promotion)

        entity.id shouldBe 31L
        entity.promotionPhoto shouldBe "https://example.com/internal.png"
        entity.performanceId shouldBe null
        entity.redirectUrl shouldBe "/notices/31"
        entity.isExternal shouldBe false
        entity.carouselNumber shouldBe CarouselNumber.ONE
    }
})
