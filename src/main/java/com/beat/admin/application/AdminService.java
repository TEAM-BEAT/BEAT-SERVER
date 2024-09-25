package com.beat.admin.application;

import com.beat.admin.application.dto.request.CarouselProcessRequest.PromotionGenerateRequest;
import com.beat.admin.application.dto.request.CarouselProcessRequest.PromotionModifyRequest;
import com.beat.admin.port.in.AdminUseCase;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.port.in.PerformanceUseCase;
import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.port.in.PromotionUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService implements AdminUseCase {

	private final PromotionUseCase promotionUseCase;
	private final PerformanceUseCase performanceUseCase;

	@Override
	@Transactional(readOnly = true)
	public List<Promotion> findAllPromotionsSortedByCarouselNumber() {
		List<Promotion> promotions = promotionUseCase.findAllPromotions();
		return sortPromotionsByCarouselNumber(promotions);
	}

	@Override
	@Transactional
	public List<Promotion> processPromotionsAndSortByCarouselNumber(List<PromotionModifyRequest> modifyRequests,
		List<PromotionGenerateRequest> generateRequests, List<CarouselNumber> deleteCarouselNumbers) {

		List<Promotion> modifiedPromotions = handlePromotionModification(modifyRequests);
		List<Promotion> addedPromotions = handlePromotionGeneration(generateRequests);
		handlePromotionDeletion(deleteCarouselNumbers);

		List<Promotion> applyPromotionChanges = new ArrayList<>(modifiedPromotions);
		applyPromotionChanges.addAll(addedPromotions);

		return sortPromotionsByCarouselNumber(applyPromotionChanges);
	}

	private List<Promotion> handlePromotionModification(List<PromotionModifyRequest> modifyRequests) {
		return modifyRequests.stream().map(modifyRequest -> {
			Promotion promotion = promotionUseCase.findById(modifyRequest.promotionId());
			Performance performance = Optional.ofNullable(modifyRequest.performanceId())
				.map(performanceUseCase::findById)
				.orElse(null);

			return promotionUseCase.modifyPromotion(promotion, performance, modifyRequest);
		}).toList();
	}

	private List<Promotion> handlePromotionGeneration(List<PromotionGenerateRequest> generateRequests) {
		return generateRequests.stream().map(generateRequest -> {
			Performance performance = Optional.ofNullable(generateRequest.performanceId())
				.map(performanceUseCase::findById)
				.orElse(null);

			return promotionUseCase.createPromotion(generateRequest.newImageUrl(), performance,
				generateRequest.redirectUrl(), generateRequest.isExternal(), generateRequest.carouselNumber());
		}).toList();
	}

	private void handlePromotionDeletion(List<CarouselNumber> deleteCarouselNumbers) {
		deleteCarouselNumbers.forEach(promotionUseCase::deleteByCarouselNumber);
	}

	private List<Promotion> sortPromotionsByCarouselNumber(List<Promotion> promotions) {
		return promotions.stream()
			.sorted(Comparator.comparing(Promotion::getCarouselNumber, Comparator.comparingInt(Enum::ordinal)))
			.toList();
	}
}