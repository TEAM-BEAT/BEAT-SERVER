package com.beat.admin.promotion.application.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.admin.exception.AdminApplicationException;
import com.beat.admin.promotion.application.command.CarouselHandleCommand.PromotionGenerateCommand;
import com.beat.admin.promotion.application.command.CarouselHandleCommand.PromotionModifyCommand;
import com.beat.admin.promotion.application.result.AdminPromotionResults;
import com.beat.admin.promotion.application.result.AdminPromotionResults.AdminPromotionResult;
import com.beat.admin.promotion.exception.PromotionApplicationErrorCode;
import com.beat.contracts.cdn.ImageCachePort;
import com.beat.contracts.performance.PerformanceSummaryReadPort;
import com.beat.contracts.storage.FileStoragePort;
import com.beat.contracts.storage.ImageObjectMetadata;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.promotion.model.CarouselNumber;
import com.beat.domain.promotion.model.Promotion;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.domain.promotion.service.PromotionCarouselDomainService;
import com.beat.global.support.utils.ImageKeyExtractor;

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
	private final PerformanceSummaryReadPort performanceSummaryReadPort;
	private final ImageCachePort imageCachePort;
	private final FileStoragePort fileStoragePort;
	private final PromotionCarouselDomainService promotionCarouselDomainService;

	@Transactional
	public AdminPromotionResults processAllPromotionsSortedByCarouselNumber(Long memberId,
		CarouselHandleCommand command) {
		validateMemberExists(memberId);

		ClassifiedCarouselPromotions classifiedPromotions = classifyCarouselPromotions(command);
		validateCarouselAssignments(classifiedPromotions);
		validateCarouselImageObjects(classifiedPromotions);

		List<Promotion> allExistingPromotions = promotionRepository.findAll();
		List<Long> deletePromotionIds = extractDeletePromotionIds(allExistingPromotions,
			classifiedPromotions.requestPromotionIds());
		List<Promotion> changedPromotions = processPromotions(classifiedPromotions.modifyRequests(),
			classifiedPromotions.generateRequests(), deletePromotionIds);

		return toPromotionResults(changedPromotions);
	}

	private void validateCarouselImageObjects(ClassifiedCarouselPromotions promotions) {
		promotions.modifyRequests().stream()
			.map(PromotionModifyCommand::newImageUrl)
			.forEach(this::validateCarouselImageObject);
		promotions.generateRequests().stream()
			.map(PromotionGenerateCommand::newImageUrl)
			.forEach(this::validateCarouselImageObject);
	}

	private void validateCarouselImageObject(String imageUrl) {
		String imageKey = ImageKeyExtractor.extract(imageUrl);
		ImageObjectMetadata metadata = fileStoragePort.findCarouselImageObjectMetadata(imageKey);
		if (metadata == null) {
			throw new AdminApplicationException(PromotionApplicationErrorCode.INVALID_IMAGE_UPLOAD);
		}
	}

	private AdminPromotionResults toPromotionResults(List<Promotion> domainPromotions) {
		List<AdminPromotionResult> promotionResults = domainPromotions.stream()
			.sorted(BY_CAROUSEL_NUMBER)
			.map(this::toPromotionResult)
			.toList();
		return AdminPromotionResults.from(promotionResults);
	}

	private AdminPromotionResult toPromotionResult(Promotion domainPromotion) {
		CarouselNumber carouselNumber = domainPromotion.getCarouselNumber();
		return AdminPromotionResult.of(
			domainPromotion.getId(),
			carouselNumber.name(),
			domainPromotion.getPromotionPhoto(),
			domainPromotion.isExternal(),
			domainPromotion.getRedirectUrl(),
			domainPromotion.getPerformanceId()
		);
	}

	private ClassifiedCarouselPromotions classifyCarouselPromotions(CarouselHandleCommand command) {
		List<PromotionModifyCommand> modifyRequests = new ArrayList<>();
		List<PromotionGenerateCommand> generateRequests = new ArrayList<>();
		Set<Long> requestPromotionIds = new HashSet<>();

		for (PromotionHandleCommand promotionCommand : command.carousels()) {
			switch (promotionCommand) {
				case PromotionModifyCommand modifyCommand -> {
					if (!requestPromotionIds.add(modifyCommand.promotionId())) {
						throw new AdminApplicationException(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT);
					}
					modifyRequests.add(modifyCommand);
				}
				case PromotionGenerateCommand generateCommand -> generateRequests.add(generateCommand);
				case null, default ->
					throw new AdminApplicationException(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT);
			}
		}

		return new ClassifiedCarouselPromotions(
			List.copyOf(modifyRequests),
			List.copyOf(generateRequests),
			new HashSet<>(requestPromotionIds)
		);
	}

	private void validateCarouselAssignments(ClassifiedCarouselPromotions promotions) {
		List<CarouselNumber> carouselNumbers = new ArrayList<>();
		promotions.modifyRequests().stream()
			.map(PromotionModifyCommand::carouselNumber)
			.map(this::toCarouselNumber)
			.forEach(carouselNumbers::add);
		promotions.generateRequests().stream()
			.map(PromotionGenerateCommand::carouselNumber)
			.map(this::toCarouselNumber)
			.forEach(carouselNumbers::add);

		if (!promotionCarouselDomainService.hasValidCarouselAssignments(carouselNumbers)) {
			throw new AdminApplicationException(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT);
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

	private List<Promotion> processPromotions(List<PromotionModifyCommand> modifyRequests,
		List<PromotionGenerateCommand> generateRequests, List<Long> deletePromotionIds) {
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

	private List<Promotion> handlePromotionModification(List<PromotionModifyCommand> modifyRequests) {
		return modifyRequests.stream()
			.map(modifyRequest -> {
				Promotion promotion = findPromotionById(modifyRequest.promotionId());
				Long performanceId = validatePerformanceId(modifyRequest.performanceId());

				String imageKey = ImageKeyExtractor.extract(modifyRequest.newImageUrl());
				Promotion updatedPromotion = promotion.updatePromotionDetails(
					toCarouselNumber(modifyRequest.carouselNumber()),
					imageKey, modifyRequest.isExternal(),
					modifyRequest.redirectUrl(),
					performanceId);
				Promotion saved = promotionRepository.save(updatedPromotion);
				imageCachePort.preWarm(imageKey);
				return saved;
			})
			.toList();
	}

	private List<Promotion> handlePromotionGeneration(List<PromotionGenerateCommand> generateRequests) {
		return generateRequests.stream()
			.map(generateRequest -> {
				Long performanceId = validatePerformanceId(generateRequest.performanceId());

				String imageKey = ImageKeyExtractor.extract(generateRequest.newImageUrl());
				Promotion newPromotion = Promotion.create(imageKey,
					performanceId,
					generateRequest.redirectUrl(), generateRequest.isExternal(),
					toCarouselNumber(generateRequest.carouselNumber()));
				Promotion saved = promotionRepository.save(newPromotion);
				imageCachePort.preWarm(imageKey);
				return saved;
			})
			.toList();
	}

	private CarouselNumber toCarouselNumber(String carouselNumber) {
		return CarouselNumber.valueOf(carouselNumber);
	}

	private Promotion findPromotionById(Long promotionId) {
		return promotionRepository.findById(promotionId)
			.orElseThrow(() -> new AdminApplicationException(PromotionApplicationErrorCode.PROMOTION_NOT_FOUND));
	}

	private Long validatePerformanceId(Long performanceId) {
		if (performanceId == null) {
			return null;
		}
		performanceSummaryReadPort.findById(performanceId)
			.orElseThrow(() -> new AdminApplicationException(PromotionApplicationErrorCode.PERFORMANCE_NOT_FOUND));
		return performanceId;
	}

	private void validateMemberExists(Long memberId) {
		memberRepository.findById(memberId)
			.orElseThrow(() -> new AdminApplicationException(PromotionApplicationErrorCode.MEMBER_NOT_FOUND));
	}

	private record ClassifiedCarouselPromotions(
		List<PromotionModifyCommand> modifyRequests,
		List<PromotionGenerateCommand> generateRequests,
		Set<Long> requestPromotionIds
	) {
	}
}
