package com.beat.admin.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.admin.application.dto.request.CarouselHandleRequest.PromotionGenerateRequest;
import com.beat.admin.application.dto.request.CarouselHandleRequest.PromotionModifyRequest;
import com.beat.admin.port.in.AdminUseCase;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.exception.PromotionErrorCode;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService implements AdminUseCase {

	private final PromotionRepository promotionRepository;
	private final PerformanceRepository performanceRepository;

	@Override
	@Transactional(readOnly = true)
	public List<Promotion> findAllPromotionsSortedByCarouselNumber() {
		List<Promotion> promotions = promotionRepository.findAll();
		return sortPromotionsByCarouselNumber(promotions);
	}

	@Override
	@Transactional
	public List<Promotion> processPromotionsAndSortByPromotionId(List<PromotionModifyRequest> modifyRequests,
		List<PromotionGenerateRequest> generateRequests, List<Long> deletePromotionIds) {

		handlePromotionDeletion(deletePromotionIds);
		List<Promotion> modifiedPromotions = handlePromotionModification(modifyRequests);
		List<Promotion> addedPromotions = handlePromotionGeneration(generateRequests);

		List<Promotion> applyPromotionChanges = new ArrayList<>(modifiedPromotions);
		applyPromotionChanges.addAll(addedPromotions);

		return sortPromotionsByCarouselNumber(applyPromotionChanges);
	}

	private void handlePromotionDeletion(List<Long> deletePromotionIds) {
		if (!deletePromotionIds.isEmpty()) {
			promotionRepository.deleteByPromotionIds(deletePromotionIds);
		}
	}

	private List<Promotion> handlePromotionModification(List<PromotionModifyRequest> modifyRequests) {
		return modifyRequests.stream()
			.map(modifyRequest -> {

				Promotion promotion = findPromotionById(modifyRequest.promotionId());

				Long performanceId = validatePerformanceId(modifyRequest.performanceId());

				Promotion updatedPromotion = promotion.updatePromotionDetails(modifyRequest.carouselNumber(),
					modifyRequest.newImageUrl(), modifyRequest.isExternal(), modifyRequest.redirectUrl(), performanceId);
				return promotionRepository.save(updatedPromotion);
			})
			.toList();
	}

	private List<Promotion> handlePromotionGeneration(List<PromotionGenerateRequest> generateRequests) {
		return generateRequests.stream()
			.map(generateRequest -> {
				Long performanceId = validatePerformanceId(generateRequest.performanceId());

				Promotion newPromotion = Promotion.create(generateRequest.newImageUrl(), performanceId,
					generateRequest.redirectUrl(), generateRequest.isExternal(), generateRequest.carouselNumber());
				return promotionRepository.save(newPromotion);
			})
			.toList();
	}

	private Promotion findPromotionById(Long promotionId) {
		return promotionRepository.findById(promotionId)
			.orElseThrow(() -> new NotFoundException(PromotionErrorCode.PROMOTION_NOT_FOUND));
	}

	private Long validatePerformanceId(Long performanceId) {
		if (performanceId == null) {
			return null;
		}
		performanceRepository.findById(performanceId)
			.orElseThrow(() -> new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
		return performanceId;
	}

	private List<Promotion> sortPromotionsByCarouselNumber(List<Promotion> promotions) {
		return promotions.stream()
			.sorted(Comparator.comparing(Promotion::getCarouselNumber, Comparator.comparingInt(Enum::ordinal)))
			.toList();
	}
}
