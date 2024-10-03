package com.beat.admin.facade;

import com.beat.admin.application.dto.request.PromotionHandleRequest;
import com.beat.admin.application.dto.response.CarouselFindAllResponse;
import com.beat.admin.application.dto.response.UserFindAllResponse;
import com.beat.admin.application.dto.request.CarouselHandleRequest;
import com.beat.admin.application.dto.request.CarouselHandleRequest.PromotionGenerateRequest;
import com.beat.admin.application.dto.request.CarouselHandleRequest.PromotionModifyRequest;
import com.beat.admin.application.dto.response.CarouselHandleAllResponse;
import com.beat.admin.port.in.AdminUseCase;
import com.beat.domain.member.port.in.MemberUseCase;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.port.in.PromotionUseCase;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.port.in.UserUseCase;
import com.beat.global.external.s3.application.dto.BannerPresignedUrlFindResponse;
import com.beat.global.external.s3.application.dto.CarouselPresignedUrlFindAllResponse;
import com.beat.global.external.s3.port.in.FileUseCase;

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
	private final FileUseCase fileUseCase;
	private final AdminUseCase adminUsecase;
	private final MemberUseCase memberUseCase;
	private final UserUseCase userUseCase;
	private final PromotionUseCase promotionUseCase;

	public UserFindAllResponse checkMemberAndFindAllUsers(Long memberId) {
		memberUseCase.findMemberById(memberId);
		List<Users> users = userUseCase.findAllUsers();
		return UserFindAllResponse.from(users);
	}

	public CarouselPresignedUrlFindAllResponse checkMemberAndIssueAllPresignedUrlsForCarousel(Long memberId,
		List<String> carouselImages) {
		memberUseCase.findMemberById(memberId);
		Map<String, String> carouselPresignedUrls = fileUseCase.issueAllPresignedUrlsForCarousel(carouselImages);
		return CarouselPresignedUrlFindAllResponse.from(carouselPresignedUrls);
	}

	public BannerPresignedUrlFindResponse checkMemberAndIssuePresignedUrlForBanner(Long memberId, String bannerImage) {
		memberUseCase.findMemberById(memberId);
		String bannerPresignedUrl = fileUseCase.issuePresignedUrlForBanner(bannerImage);
		return BannerPresignedUrlFindResponse.from(bannerPresignedUrl);
	}

	public CarouselFindAllResponse checkMemberAndFindAllPromotionsSortedByCarouselNumber(Long memberId) {
		memberUseCase.findMemberById(memberId);
		List<Promotion> promotions = adminUsecase.findAllPromotionsSortedByCarouselNumber();
		return CarouselFindAllResponse.from(promotions);
	}

	public CarouselHandleAllResponse checkMemberAndProcessAllPromotionsSortedByCarouselNumber(Long memberId,
		CarouselHandleRequest request) {

		memberUseCase.findMemberById(memberId);

		List<PromotionModifyRequest> modifyRequests = new ArrayList<>();
		List<PromotionGenerateRequest> generateRequests = new ArrayList<>();
		Set<Long> requestPromotionIds = new HashSet<>();

		categorizePromotionRequestsByPromotionId(request, modifyRequests, generateRequests, requestPromotionIds);

		List<Promotion> allExistingPromotions = promotionUseCase.findAllPromotions();

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
}