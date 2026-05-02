package com.beat.admin.facade;

import java.util.List;

import org.springframework.stereotype.Service;

import com.beat.admin.application.dto.request.CarouselHandleRequest;
import com.beat.admin.application.dto.response.BannerPresignedUrlFindResponse;
import com.beat.admin.application.dto.response.CarouselFindAllResponse;
import com.beat.admin.application.dto.response.CarouselHandleAllResponse;
import com.beat.admin.application.dto.response.CarouselPresignedUrlFindAllResponse;
import com.beat.admin.application.dto.response.UserFindAllResponse;
import com.beat.admin.application.service.command.AdminCommandService;
import com.beat.admin.application.service.query.AdminQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminFacade {

	private final AdminQueryService adminQueryService;
	private final AdminCommandService adminCommandService;

	public UserFindAllResponse checkMemberAndFindAllUsers(Long memberId) {
		return adminQueryService.findAllUsers(memberId);
	}

	public CarouselPresignedUrlFindAllResponse checkMemberAndIssueAllPresignedUrlsForCarousel(Long memberId,
		List<String> carouselImages) {
		return adminQueryService.issueAllPresignedUrlsForCarousel(memberId, carouselImages);
	}

	public BannerPresignedUrlFindResponse checkMemberAndIssuePresignedUrlForBanner(Long memberId, String bannerImage) {
		return adminQueryService.issuePresignedUrlForBanner(memberId, bannerImage);
	}

	public CarouselFindAllResponse checkMemberAndFindAllPromotionsSortedByCarouselNumber(Long memberId) {
		return adminQueryService.findAllPromotionsSortedByCarouselNumber(memberId);
	}

	public CarouselHandleAllResponse checkMemberAndProcessAllPromotionsSortedByCarouselNumber(Long memberId,
		CarouselHandleRequest request) {
		return adminCommandService.processAllPromotionsSortedByCarouselNumber(memberId, request);
	}
}
