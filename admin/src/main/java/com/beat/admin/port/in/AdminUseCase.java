package com.beat.admin.port.in;

import com.beat.admin.application.dto.request.CarouselHandleRequest.PromotionGenerateRequest;
import com.beat.admin.application.dto.request.CarouselHandleRequest.PromotionModifyRequest;
import com.beat.domain.promotion.domain.Promotion;

import java.util.List;

public interface AdminUseCase {
	List<Promotion> findAllPromotionsSortedByCarouselNumber();

	List<Promotion> processPromotionsAndSortByPromotionId(List<PromotionModifyRequest> modifyRequests,
		List<PromotionGenerateRequest> generateRequests, List<Long> deletePromotionIds);
}