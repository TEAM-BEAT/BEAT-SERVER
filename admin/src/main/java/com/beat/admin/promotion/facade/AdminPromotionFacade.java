package com.beat.admin.promotion.facade;

import java.util.List;

import org.springframework.stereotype.Service;

import com.beat.admin.exception.AdminApplicationException;
import com.beat.admin.promotion.api.request.CarouselHandleRequest;
import com.beat.admin.promotion.api.request.CarouselHandleRequest.PromotionGenerateRequest;
import com.beat.admin.promotion.api.request.CarouselHandleRequest.PromotionModifyRequest;
import com.beat.admin.promotion.api.request.PromotionHandleRequest;
import com.beat.admin.promotion.application.command.CarouselHandleCommand;
import com.beat.admin.promotion.application.command.CarouselHandleCommand.PromotionGenerateCommand;
import com.beat.admin.promotion.application.command.CarouselHandleCommand.PromotionModifyCommand;
import com.beat.admin.promotion.application.command.PromotionHandleCommand;
import com.beat.admin.promotion.api.response.BannerPresignedUrlFindResponse;
import com.beat.admin.promotion.api.response.CarouselFindAllResponse;
import com.beat.admin.promotion.api.response.CarouselHandleAllResponse;
import com.beat.admin.promotion.api.response.CarouselPresignedUrlFindAllResponse;
import com.beat.admin.promotion.application.command.AdminPromotionCommandService;
import com.beat.admin.promotion.application.query.AdminPromotionQueryService;
import com.beat.admin.promotion.exception.PromotionApplicationErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminPromotionFacade {

	private final AdminPromotionQueryService adminPromotionQueryService;
	private final AdminPromotionCommandService adminPromotionCommandService;

	public CarouselPresignedUrlFindAllResponse checkMemberAndIssueAllPresignedUrlsForCarousel(Long memberId,
		List<String> carouselImages) {
		return CarouselPresignedUrlFindAllResponse.from(
			adminPromotionQueryService.issueAllPresignedUrlsForCarousel(memberId, carouselImages));
	}

	public BannerPresignedUrlFindResponse checkMemberAndIssuePresignedUrlForBanner(Long memberId, String bannerImage) {
		return BannerPresignedUrlFindResponse.from(
			adminPromotionQueryService.issuePresignedUrlForBanner(memberId, bannerImage));
	}

	public CarouselFindAllResponse checkMemberAndFindAllPromotionsSortedByCarouselNumber(Long memberId) {
		return CarouselFindAllResponse.from(
			adminPromotionQueryService.findAllPromotionsSortedByCarouselNumber(memberId));
	}

	public CarouselHandleAllResponse checkMemberAndProcessAllPromotionsSortedByCarouselNumber(Long memberId,
		CarouselHandleRequest request) {
		return CarouselHandleAllResponse.from(
			adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(memberId, toCommand(request)));
	}

	private CarouselHandleCommand toCommand(CarouselHandleRequest request) {
		if (request == null || request.carousels() == null) {
			throw invalidRequest();
		}
		return new CarouselHandleCommand(request.carousels().stream()
			.map(this::toCommand)
			.toList());
	}

	private PromotionHandleCommand toCommand(PromotionHandleRequest request) {
		return switch (request) {
			case PromotionModifyRequest modifyRequest -> new PromotionModifyCommand(
				requireNonNull(modifyRequest.promotionId()),
				requireNonNull(modifyRequest.carouselNumber()).name(),
				requireNonNull(modifyRequest.newImageUrl()),
				requireNonNull(modifyRequest.isExternal()),
				requireNonNull(modifyRequest.redirectUrl()),
				modifyRequest.performanceId()
			);
			case PromotionGenerateRequest generateRequest -> new PromotionGenerateCommand(
				requireNonNull(generateRequest.carouselNumber()).name(),
				requireNonNull(generateRequest.newImageUrl()),
				requireNonNull(generateRequest.isExternal()),
				requireNonNull(generateRequest.redirectUrl()),
				generateRequest.performanceId()
			);
			case null, default -> throw invalidRequest();
		};
	}

	private <T> T requireNonNull(T value) {
		if (value == null) {
			throw invalidRequest();
		}
		return value;
	}

	private AdminApplicationException invalidRequest() {
		return new AdminApplicationException(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT);
	}
}
