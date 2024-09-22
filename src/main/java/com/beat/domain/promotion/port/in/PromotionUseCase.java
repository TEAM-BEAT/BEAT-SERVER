package com.beat.domain.promotion.port.in;

import com.beat.domain.admin.application.dto.request.CarouselProcessRequest.PromotionModifyRequest;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;

import java.util.List;

public interface PromotionUseCase {
	Promotion findById(Long promotionId);

	List<Promotion> findAllPromotions();

	Promotion createPromotion(String newImageUrl, Performance performance, String redirectUrl, boolean isExternal,
		CarouselNumber carouselNumber);

	Promotion modifyPromotion(Promotion promotion, Performance performance, PromotionModifyRequest request);

	void deleteByCarouselNumber(CarouselNumber carouselNumber);

	List<CarouselNumber> findAllCarouselNumbers();
}