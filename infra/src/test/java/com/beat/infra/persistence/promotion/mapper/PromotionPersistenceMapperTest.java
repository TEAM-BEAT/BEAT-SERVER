package com.beat.infra.persistence.promotion.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.infra.persistence.promotion.entity.PromotionJpaEntity;

class PromotionPersistenceMapperTest {

	private final PromotionPersistenceMapper mapper = new PromotionPersistenceMapper();

	@Test
	void toDomainPreservesJpaEntityFieldsUsedByJavaCallers() {
		PromotionJpaEntity entity = PromotionJpaEntity.rehydrate(
			11L,
			"https://example.com/promotion.png",
			22L,
			"https://example.com/performance",
			true,
			CarouselNumber.THREE
		);

		Promotion promotion = mapper.toDomain(entity);

		assertAll(
			() -> assertEquals(11L, promotion.getId()),
			() -> assertEquals("https://example.com/promotion.png", promotion.getPromotionPhoto()),
			() -> assertEquals(22L, promotion.getPerformanceId()),
			() -> assertEquals("https://example.com/performance", promotion.getRedirectUrl()),
			() -> assertTrue(promotion.isExternal()),
			() -> assertEquals(CarouselNumber.THREE, promotion.getCarouselNumber())
		);
	}

	@Test
	void toEntityPreservesDomainFieldsAndJavaVisiblePromotionJpaEntityContract() {
		Promotion promotion = Promotion.rehydrate(
			31L,
			"https://example.com/internal.png",
			null,
			"/notices/31",
			false,
			CarouselNumber.ONE
		);

		PromotionJpaEntity entity = mapper.toEntity(promotion);

		assertAll(
			() -> assertEquals(31L, entity.getId()),
			() -> assertEquals("https://example.com/internal.png", entity.getPromotionPhoto()),
			() -> assertNull(entity.getPerformanceId()),
			() -> assertEquals("/notices/31", entity.getRedirectUrl()),
			() -> assertFalse(entity.isExternal()),
			() -> assertEquals(CarouselNumber.ONE, entity.getCarouselNumber())
		);
	}
}
