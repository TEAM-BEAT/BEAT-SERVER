package com.beat.admin.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.admin.application.dto.request.CarouselHandleRequest;
import com.beat.admin.application.dto.request.CarouselHandleRequest.PromotionGenerateRequest;
import com.beat.admin.application.dto.request.CarouselHandleRequest.PromotionModifyRequest;
import com.beat.admin.application.dto.request.PromotionHandleRequest;
import com.beat.admin.application.dto.response.CarouselHandleAllResponse;
import com.beat.admin.application.exception.AdminApplicationErrorCode;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminCommandService {

	private final MemberRepository memberRepository;
	private final PromotionRepository promotionRepository;
	private final PerformanceRepository performanceRepository;

	@Transactional
	public CarouselHandleAllResponse processAllPromotionsSortedByCarouselNumber(Long memberId,
		CarouselHandleRequest request) {
		validateMemberExists(memberId);

		List<PromotionModifyRequest> modifyRequests = new ArrayList<>();
		List<PromotionGenerateRequest> generateRequests = new ArrayList<>();
		Set<Long> requestPromotionIds = new HashSet<>();

		categorizePromotionRequestsByPromotionId(request, modifyRequests, generateRequests, requestPromotionIds);

		List<Promotion> allExistingPromotions = promotionRepository.findAll();
		List<Long> deletePromotionIds = extractDeletePromotionIds(allExistingPromotions, requestPromotionIds);
		List<Promotion> changedPromotions = processPromotions(modifyRequests, generateRequests, deletePromotionIds);

		return CarouselHandleAllResponse.from(
			AdminPromotionResults.fromSortedByCarouselNumber(changedPromotions)
		);
	}

	private void categorizePromotionRequestsByPromotionId(CarouselHandleRequest request,
		List<PromotionModifyRequest> modifyRequests, List<PromotionGenerateRequest> generateRequests,
		Set<Long> requestPromotionIds) {

		for (PromotionHandleRequest promotionRequest : request.carousels()) {
			if (promotionRequest instanceof PromotionModifyRequest modifyRequest) {
				modifyRequests.add(modifyRequest);
				requestPromotionIds.add(modifyRequest.promotionId());
			} else if (promotionRequest instanceof PromotionGenerateRequest generateRequest) {
				generateRequests.add(generateRequest);
			}
		}
	}

	private List<Long> extractDeletePromotionIds(List<Promotion> allExistingPromotions, Set<Long> requestPromotionIds) {
		Set<Long> allExistingPromotionIds = allExistingPromotions.stream()
			.map(Promotion::getId)
			.collect(Collectors.toSet());

		return allExistingPromotionIds.stream()
			.filter(existingId -> !requestPromotionIds.contains(existingId))
			.toList();
	}

	private List<Promotion> processPromotions(List<PromotionModifyRequest> modifyRequests,
		List<PromotionGenerateRequest> generateRequests, List<Long> deletePromotionIds) {
		handlePromotionDeletion(deletePromotionIds);
		List<Promotion> modifiedPromotions = handlePromotionModification(modifyRequests);
		List<Promotion> addedPromotions = handlePromotionGeneration(generateRequests);

		List<Promotion> applyPromotionChanges = new ArrayList<>(modifiedPromotions);
		applyPromotionChanges.addAll(addedPromotions);

		return applyPromotionChanges;
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
			.orElseThrow(() -> new NotFoundException(AdminApplicationErrorCode.PROMOTION_NOT_FOUND));
	}

	private Long validatePerformanceId(Long performanceId) {
		if (performanceId == null) {
			return null;
		}
		performanceRepository.findById(performanceId)
			.orElseThrow(() -> new NotFoundException(AdminApplicationErrorCode.PERFORMANCE_NOT_FOUND));
		return performanceId;
	}

	private void validateMemberExists(Long memberId) {
		memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(AdminApplicationErrorCode.MEMBER_NOT_FOUND));
	}
}
