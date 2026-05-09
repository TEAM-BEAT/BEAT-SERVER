package com.beat.admin.promotion.application.service.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.admin.common.application.converter.AdminCarouselNumberEnumConverter;

import com.beat.admin.promotion.application.dto.request.AdminCarouselNumber;
import com.beat.admin.promotion.application.dto.request.CarouselHandleRequest;
import com.beat.admin.promotion.application.dto.request.CarouselHandleRequest.PromotionGenerateRequest;
import com.beat.admin.promotion.application.dto.request.CarouselHandleRequest.PromotionModifyRequest;
import com.beat.admin.promotion.application.dto.request.PromotionHandleRequest;
import com.beat.admin.promotion.application.dto.response.CarouselHandleAllResponse;
import com.beat.admin.promotion.application.dto.result.AdminPromotionResults;
import com.beat.admin.promotion.application.dto.result.AdminPromotionResults.AdminPromotionResult;
import com.beat.admin.application.exception.AdminApplicationErrorCode;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.global.support.exception.BadRequestException;
import com.beat.global.support.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminPromotionCommandService {

	private static final Comparator<Promotion> BY_CAROUSEL_NUMBER = Comparator.comparing(
		Promotion::getCarouselNumber,
		Comparator.comparingInt(Enum::ordinal)
	);

	private final MemberRepository memberRepository;
	private final PromotionRepository promotionRepository;
	private final PerformanceRepository performanceRepository;

	@Transactional
	public CarouselHandleAllResponse processAllPromotionsSortedByCarouselNumber(Long memberId,
		CarouselHandleRequest request) {
		validateMemberExists(memberId);

		ClassifiedCarouselPromotions classifiedPromotions = classifyCarouselPromotions(request);

		List<Promotion> allExistingPromotions = promotionRepository.findAll();
		List<Long> deletePromotionIds = extractDeletePromotionIds(allExistingPromotions,
			classifiedPromotions.requestPromotionIds());
		List<Promotion> changedPromotions = processPromotions(classifiedPromotions.modifyRequests(),
			classifiedPromotions.generateRequests(), deletePromotionIds);

		return CarouselHandleAllResponse.from(
			toPromotionResults(changedPromotions)
		);
	}

	private AdminPromotionResults toPromotionResults(List<Promotion> domainPromotions) {
		List<AdminPromotionResult> promotionResults = domainPromotions.stream()
			.sorted(BY_CAROUSEL_NUMBER)
			.map(this::toPromotionResult)
			.toList();
		return AdminPromotionResults.from(promotionResults);
	}

	private AdminPromotionResult toPromotionResult(Promotion domainPromotion) {
		return AdminPromotionResult.of(
			domainPromotion.getId(),
			AdminCarouselNumberEnumConverter.toApiName(domainPromotion.getCarouselNumber()),
			domainPromotion.getPromotionPhoto(),
			domainPromotion.isExternal(),
			domainPromotion.getRedirectUrl(),
			domainPromotion.getPerformanceId()
		);
	}

	private void validateCarouselHandleRequest(CarouselHandleRequest request) {
		if (request == null || request.carousels() == null) {
			throw new BadRequestException(AdminApplicationErrorCode.INVALID_REQUEST_FORMAT);
		}
	}

	private ClassifiedCarouselPromotions classifyCarouselPromotions(CarouselHandleRequest request) {
		validateCarouselHandleRequest(request);

		List<PromotionModifyRequest> modifyRequests = new ArrayList<>();
		List<PromotionGenerateRequest> generateRequests = new ArrayList<>();
		Set<Long> requestPromotionIds = new HashSet<>();

		for (PromotionHandleRequest promotionRequest : request.carousels()) {
			switch (promotionRequest) {
				case PromotionModifyRequest modifyRequest -> {
					modifyRequests.add(modifyRequest);
					requestPromotionIds.add(modifyRequest.promotionId());
				}
				case PromotionGenerateRequest generateRequest -> generateRequests.add(generateRequest);
				case null, default -> throw new BadRequestException(AdminApplicationErrorCode.INVALID_REQUEST_FORMAT);
			}
		}

		return new ClassifiedCarouselPromotions(
			List.copyOf(modifyRequests),
			List.copyOf(generateRequests),
			new HashSet<>(requestPromotionIds)
		);
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
		List<Promotion> modifiedDomainPromotions = handlePromotionModification(modifyRequests);
		List<Promotion> addedPromotions = handlePromotionGeneration(generateRequests);

		List<Promotion> appliedDomainPromotionChanges = new ArrayList<>(modifiedDomainPromotions);
		appliedDomainPromotionChanges.addAll(addedPromotions);

		return appliedDomainPromotionChanges;
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

				Promotion updatedPromotion = promotion.updatePromotionDetails(
					toCarouselNumber(modifyRequest.carouselNumber()),
					modifyRequest.newImageUrl(), modifyRequest.isExternal(), modifyRequest.redirectUrl(),
					performanceId);
				return promotionRepository.save(updatedPromotion);
			})
			.toList();
	}

	private List<Promotion> handlePromotionGeneration(List<PromotionGenerateRequest> generateRequests) {
		return generateRequests.stream()
			.map(generateRequest -> {
				Long performanceId = validatePerformanceId(generateRequest.performanceId());

				Promotion newPromotion = Promotion.create(generateRequest.newImageUrl(), performanceId,
					generateRequest.redirectUrl(), generateRequest.isExternal(),
					toCarouselNumber(generateRequest.carouselNumber()));
				return promotionRepository.save(newPromotion);
			})
			.toList();
	}

	private CarouselNumber toCarouselNumber(AdminCarouselNumber carouselNumber) {
		if (carouselNumber == null) {
			return null;
		}
		return AdminCarouselNumberEnumConverter.toDomain(carouselNumber);
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

	private record ClassifiedCarouselPromotions(
		List<PromotionModifyRequest> modifyRequests,
		List<PromotionGenerateRequest> generateRequests,
		Set<Long> requestPromotionIds
	) {
	}
}
