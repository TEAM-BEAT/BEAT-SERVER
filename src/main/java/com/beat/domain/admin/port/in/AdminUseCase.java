package com.beat.domain.admin.port.in;

import com.beat.domain.admin.application.dto.request.CarouselProcessRequest.PromotionGenerateRequest;
import com.beat.domain.admin.application.dto.request.CarouselProcessRequest.PromotionModifyRequest;
import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;

import java.util.List;

public interface AdminUseCase {
	List<Promotion> findAllPromotionsSortedByCarouselNumber();

	List<Promotion> handlePromotions(List<PromotionModifyRequest> modifyRequests,
		List<PromotionGenerateRequest> generateRequests, List<CarouselNumber> deleteCarouselNumbers);
}