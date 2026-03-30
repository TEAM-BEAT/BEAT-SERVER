package com.beat.admin.application;

import com.beat.admin.application.dto.request.CarouselHandleRequest.PromotionGenerateRequest;
import com.beat.admin.application.dto.request.CarouselHandleRequest.PromotionModifyRequest;
import com.beat.admin.port.in.AdminUseCase;
import com.beat.domain.performance.dao.PerformanceRepository;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.promotion.dao.PromotionRepository;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.exception.PromotionErrorCode;
import com.beat.domain.promotion.port.in.PromotionModifyCommand;
import com.beat.global.common.exception.NotFoundException;

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

				Performance performance = Optional.ofNullable(modifyRequest.performanceId())
					.map(this::findPerformanceById)
					.orElse(null);

				PromotionModifyCommand command = new PromotionModifyCommand(
					modifyRequest.promotionId(),
					modifyRequest.carouselNumber(),
					modifyRequest.newImageUrl(),
					modifyRequest.isExternal(),
					modifyRequest.redirectUrl(),
					modifyRequest.performanceId()
				);

				promotion.updatePromotionDetails(command.carouselNumber(), command.newImageUrl(), command.isExternal(),
					command.redirectUrl(), performance);
				return promotionRepository.save(promotion);
			})
			.toList();
	}

	private List<Promotion> handlePromotionGeneration(List<PromotionGenerateRequest> generateRequests) {
		return generateRequests.stream()
			.map(generateRequest -> {
				Performance performance = Optional.ofNullable(generateRequest.performanceId())
					.map(this::findPerformanceById)
					.orElse(null);

				Promotion newPromotion = Promotion.create(generateRequest.newImageUrl(), performance,
					generateRequest.redirectUrl(), generateRequest.isExternal(), generateRequest.carouselNumber());
				return promotionRepository.save(newPromotion);
			})
			.toList();
	}

	private Promotion findPromotionById(Long promotionId) {
		return promotionRepository.findById(promotionId)
			.orElseThrow(() -> new NotFoundException(PromotionErrorCode.PROMOTION_NOT_FOUND));
	}

	private Performance findPerformanceById(Long performanceId) {
		return performanceRepository.findById(performanceId)
			.orElseThrow(() -> new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
	}

	private List<Promotion> sortPromotionsByCarouselNumber(List<Promotion> promotions) {
		return promotions.stream()
			.sorted(Comparator.comparing(Promotion::getCarouselNumber, Comparator.comparingInt(Enum::ordinal)))
			.toList();
	}
}
