package com.beat.admin.facade;

import com.beat.contracts.storage.FileStoragePort;
import com.beat.admin.application.dto.request.PromotionHandleRequest;
import com.beat.admin.application.dto.response.BannerPresignedUrlFindResponse;
import com.beat.admin.application.dto.response.CarouselFindAllResponse;
import com.beat.admin.application.dto.response.UserFindAllResponse;
import com.beat.admin.application.dto.request.CarouselHandleRequest;
import com.beat.admin.application.dto.request.CarouselHandleRequest.PromotionGenerateRequest;
import com.beat.admin.application.dto.request.CarouselHandleRequest.PromotionModifyRequest;
import com.beat.admin.application.dto.response.CarouselPresignedUrlFindAllResponse;
import com.beat.admin.application.dto.response.CarouselHandleAllResponse;
import com.beat.admin.port.in.AdminUseCase;
import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Users;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminFacade {
	private final FileStoragePort fileStoragePort;
	private final AdminUseCase adminUsecase;
	private final MemberRepository memberRepository;
	private final UserRepository userRepository;

	public UserFindAllResponse checkMemberAndFindAllUsers(Long memberId) {
		validateMemberExists(memberId);
		List<Users> users = userRepository.findAll();
		return UserFindAllResponse.from(users);
	}

	public CarouselPresignedUrlFindAllResponse checkMemberAndIssueAllPresignedUrlsForCarousel(Long memberId,
		List<String> carouselImages) {
		validateMemberExists(memberId);
		return CarouselPresignedUrlFindAllResponse.from(
			fileStoragePort.issueAllPresignedUrlsForCarousel(carouselImages)
		);
	}

	public BannerPresignedUrlFindResponse checkMemberAndIssuePresignedUrlForBanner(Long memberId, String bannerImage) {
		validateMemberExists(memberId);
		return BannerPresignedUrlFindResponse.from(fileStoragePort.issuePresignedUrlForBanner(bannerImage));
	}

	public CarouselFindAllResponse checkMemberAndFindAllPromotionsSortedByCarouselNumber(Long memberId) {
		validateMemberExists(memberId);
		List<Promotion> promotions = adminUsecase.findAllPromotionsSortedByCarouselNumber();
		return CarouselFindAllResponse.from(promotions);
	}

	public CarouselHandleAllResponse checkMemberAndProcessAllPromotionsSortedByCarouselNumber(Long memberId,
		CarouselHandleRequest request) {

		validateMemberExists(memberId);

		List<PromotionModifyRequest> modifyRequests = new ArrayList<>();
		List<PromotionGenerateRequest> generateRequests = new ArrayList<>();
		Set<Long> requestPromotionIds = new HashSet<>();

		categorizePromotionRequestsByPromotionId(request, modifyRequests, generateRequests, requestPromotionIds);

		List<Promotion> allExistingPromotions = adminUsecase.findAllPromotionsSortedByCarouselNumber();

		List<Long> deletePromotionIds = extractDeletePromotionIds(allExistingPromotions, requestPromotionIds);

		List<Promotion> sortedPromotions = adminUsecase.processPromotionsAndSortByPromotionId(modifyRequests,
			generateRequests, deletePromotionIds);

		return CarouselHandleAllResponse.from(sortedPromotions);
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

	private void validateMemberExists(Long memberId) {
		memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));
	}
}
