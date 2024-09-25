package com.beat.admin.port.in;

import com.beat.admin.application.dto.request.CarouselProcessRequest.PromotionGenerateRequest;
import com.beat.admin.application.dto.request.CarouselProcessRequest.PromotionModifyRequest;
import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;

import java.util.List;

public interface AdminUseCase {
	List<Promotion> findAllPromotionsSortedByCarouselNumber();

	List<Promotion> processPromotionsAndSortByCarouselNumber(List<PromotionModifyRequest> modifyRequests,
		List<PromotionGenerateRequest> generateRequests, List<CarouselNumber> deleteCarouselNumbers);
}