package com.beat.admin.promotion.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.beat.admin.api.response.AdminSuccessCode;
import com.beat.admin.promotion.application.dto.request.CarouselHandleRequest;
import com.beat.admin.promotion.application.dto.response.BannerPresignedUrlFindResponse;
import com.beat.admin.promotion.application.dto.response.CarouselFindAllResponse;
import com.beat.admin.promotion.application.dto.response.CarouselHandleAllResponse;
import com.beat.admin.promotion.application.dto.response.CarouselPresignedUrlFindAllResponse;
import com.beat.admin.promotion.facade.AdminPromotionFacade;
import com.beat.gateway.security.servlet.CurrentMember;
import com.beat.global.support.response.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminPromotionController implements AdminPromotionApi {

	private final AdminPromotionFacade adminPromotionFacade;

	@Override
	@GetMapping("/carousels/presigned-url")
	public ResponseEntity<SuccessResponse<CarouselPresignedUrlFindAllResponse>> createAllCarouselPresignedUrls(
		@CurrentMember Long memberId, @RequestParam List<String> carouselImages) {
		CarouselPresignedUrlFindAllResponse response = adminPromotionFacade.checkMemberAndIssueAllPresignedUrlsForCarousel(
			memberId, carouselImages);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.CAROUSEL_PRESIGNED_URL_ISSUED, response));
	}

	@Override
	@GetMapping("/banner/presigned-url")
	public ResponseEntity<SuccessResponse<BannerPresignedUrlFindResponse>> createBannerPresignedUrl(
		@CurrentMember Long memberId, @RequestParam String bannerImage) {
		BannerPresignedUrlFindResponse response = adminPromotionFacade.checkMemberAndIssuePresignedUrlForBanner(memberId,
			bannerImage);
		return ResponseEntity.status(HttpStatus.OK)
			.body(SuccessResponse.of(AdminSuccessCode.BANNER_PRESIGNED_URL_ISSUED, response));
	}

	@Override
	@GetMapping("/carousels")
	public ResponseEntity<SuccessResponse<CarouselFindAllResponse>> readAllCarouselImages(
		@CurrentMember Long memberId) {
		CarouselFindAllResponse response = adminPromotionFacade.checkMemberAndFindAllPromotionsSortedByCarouselNumber(
			memberId);
		return ResponseEntity.status(HttpStatus.OK)
			.body(SuccessResponse.of(AdminSuccessCode.FETCH_ALL_CAROUSEL_PROMOTIONS_SUCCESS, response));
	}

	@Override
	@PutMapping("/carousels")
	public ResponseEntity<SuccessResponse<CarouselHandleAllResponse>> processCarouselImages(
		@CurrentMember Long memberId,
		@RequestBody CarouselHandleRequest request) {
		CarouselHandleAllResponse response = adminPromotionFacade.checkMemberAndProcessAllPromotionsSortedByCarouselNumber(
			memberId, request);
		return ResponseEntity.status(HttpStatus.OK)
			.body(SuccessResponse.of(AdminSuccessCode.UPDATE_ALL_CAROUSEL_PROMOTIONS_SUCCESS, response));
	}
}
