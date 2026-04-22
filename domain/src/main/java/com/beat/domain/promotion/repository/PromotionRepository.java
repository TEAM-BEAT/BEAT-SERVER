package com.beat.domain.promotion.repository;

import java.util.List;
import java.util.Optional;

import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;

public interface PromotionRepository {

	List<Promotion> findAll();

	Optional<Promotion> findById(Long promotionId);

	Promotion save(Promotion promotion);

	List<Promotion> saveAll(List<Promotion> promotions);

	void deleteByPromotionIds(List<Long> promotionIds);

	void deleteByPerformanceId(Long performanceId);

	Optional<Promotion> findByCarouselNumber(CarouselNumber carouselNumber);
}
