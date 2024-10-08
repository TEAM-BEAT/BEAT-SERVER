package com.beat.admin.adapter.in.api;

import com.beat.admin.application.dto.response.CarouselFindAllResponse;
import com.beat.admin.application.dto.request.CarouselHandleRequest;
import com.beat.admin.application.dto.response.CarouselHandleAllResponse;
import com.beat.admin.exception.AdminSuccessCode;
import com.beat.admin.application.dto.response.UserFindAllResponse;
import com.beat.admin.facade.AdminFacade;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.SuccessResponse;
import com.beat.global.external.s3.application.dto.BannerPresignedUrlFindResponse;
import com.beat.global.external.s3.application.dto.CarouselPresignedUrlFindAllResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController implements AdminApi {

	private final AdminFacade adminFacade;

	@Override
	@GetMapping("/users")
	public ResponseEntity<SuccessResponse<UserFindAllResponse>> readAllUsers(@CurrentMember Long memberId) {
		UserFindAllResponse response = adminFacade.checkMemberAndFindAllUsers(memberId);
		return ResponseEntity.status(HttpStatus.OK)
			.body(SuccessResponse.of(AdminSuccessCode.FETCH_ALL_USERS_SUCCESS, response));
	}

	@Override
	@GetMapping("/carousels/presigned-url")
	public ResponseEntity<SuccessResponse<CarouselPresignedUrlFindAllResponse>> createAllCarouselPresignedUrls(
		@CurrentMember Long memberId, @RequestParam List<String> carouselImages) {
		CarouselPresignedUrlFindAllResponse response = adminFacade.checkMemberAndIssueAllPresignedUrlsForCarousel(
			memberId, carouselImages);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.CAROUSEL_PRESIGNED_URL_ISSUED, response));
	}

	@Override
	@GetMapping("/banner/presigned-url")
	public ResponseEntity<SuccessResponse<BannerPresignedUrlFindResponse>> createBannerPresignedUrl(
		@CurrentMember Long memberId, @RequestParam String bannerImage) {
		BannerPresignedUrlFindResponse response = adminFacade.checkMemberAndIssuePresignedUrlForBanner(memberId,
			bannerImage);
		return ResponseEntity.status(HttpStatus.OK)
			.body(SuccessResponse.of(AdminSuccessCode.BANNER_PRESIGNED_URL_ISSUED, response));
	}

	@Override
	@GetMapping("/carousels")
	public ResponseEntity<SuccessResponse<CarouselFindAllResponse>> readAllCarouselImages(
		@CurrentMember Long memberId) {
		CarouselFindAllResponse response = adminFacade.checkMemberAndFindAllPromotionsSortedByCarouselNumber(memberId);
		return ResponseEntity.status(HttpStatus.OK)
			.body(SuccessResponse.of(AdminSuccessCode.FETCH_ALL_CAROUSEL_PROMOTIONS_SUCCESS, response));
	}

	@Override
	@PutMapping("/carousels")
	public ResponseEntity<SuccessResponse<CarouselHandleAllResponse>> processCarouselImages(
		@CurrentMember Long memberId,
		@RequestBody CarouselHandleRequest request) {
		CarouselHandleAllResponse response = adminFacade.checkMemberAndProcessAllPromotionsSortedByCarouselNumber(memberId, request);
		return ResponseEntity.status(HttpStatus.OK)
			.body(SuccessResponse.of(AdminSuccessCode.UPDATE_ALL_CAROUSEL_PROMOTIONS_SUCCESS, response));
	}
}