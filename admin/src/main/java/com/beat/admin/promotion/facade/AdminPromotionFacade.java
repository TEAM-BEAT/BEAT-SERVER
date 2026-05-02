package com.beat.admin.promotion.facade;

import java.util.List;

import org.springframework.stereotype.Service;

import com.beat.admin.promotion.application.dto.request.CarouselHandleRequest;
import com.beat.admin.promotion.application.dto.response.BannerPresignedUrlFindResponse;
import com.beat.admin.promotion.application.dto.response.CarouselFindAllResponse;
import com.beat.admin.promotion.application.dto.response.CarouselHandleAllResponse;
import com.beat.admin.promotion.application.dto.response.CarouselPresignedUrlFindAllResponse;
import com.beat.admin.promotion.application.service.command.AdminPromotionCommandService;
import com.beat.admin.promotion.application.service.query.AdminPromotionQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminPromotionFacade {

	private final AdminPromotionQueryService adminPromotionQueryService;
	private final AdminPromotionCommandService adminPromotionCommandService;

	public CarouselPresignedUrlFindAllResponse checkMemberAndIssueAllPresignedUrlsForCarousel(Long memberId,
		List<String> carouselImages) {
		return adminPromotionQueryService.issueAllPresignedUrlsForCarousel(memberId, carouselImages);
	}

	public BannerPresignedUrlFindResponse checkMemberAndIssuePresignedUrlForBanner(Long memberId, String bannerImage) {
		return adminPromotionQueryService.issuePresignedUrlForBanner(memberId, bannerImage);
	}

	public CarouselFindAllResponse checkMemberAndFindAllPromotionsSortedByCarouselNumber(Long memberId) {
		return adminPromotionQueryService.findAllPromotionsSortedByCarouselNumber(memberId);
	}

	public CarouselHandleAllResponse checkMemberAndProcessAllPromotionsSortedByCarouselNumber(Long memberId,
		CarouselHandleRequest request) {
		return adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(memberId, request);
	}
}
