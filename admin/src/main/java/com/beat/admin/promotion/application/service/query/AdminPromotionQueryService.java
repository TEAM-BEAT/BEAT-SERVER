package com.beat.admin.promotion.application.service.query;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.admin.common.application.converter.AdminCarouselNumberEnumConverter;

import com.beat.admin.application.exception.AdminApplicationErrorCode;
import com.beat.admin.promotion.application.dto.response.BannerPresignedUrlFindResponse;
import com.beat.admin.promotion.application.dto.response.CarouselFindAllResponse;
import com.beat.admin.promotion.application.dto.response.CarouselPresignedUrlFindAllResponse;
import com.beat.admin.promotion.application.dto.result.AdminPromotionResults;
import com.beat.admin.promotion.application.dto.result.AdminPromotionResults.AdminPromotionResult;
import com.beat.contracts.storage.FileStoragePort;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPromotionQueryService {

	private static final Comparator<Promotion> BY_CAROUSEL_NUMBER = Comparator.comparing(
		Promotion::getCarouselNumber,
		Comparator.comparingInt(Enum::ordinal)
	);

	private final FileStoragePort fileStoragePort;
	private final MemberRepository memberRepository;
	private final PromotionRepository promotionRepository;

	public CarouselPresignedUrlFindAllResponse issueAllPresignedUrlsForCarousel(Long memberId,
		List<String> carouselImages) {
		validateMemberExists(memberId);
		return CarouselPresignedUrlFindAllResponse.from(
			fileStoragePort.issueAllPresignedUrlsForCarousel(carouselImages)
		);
	}

	public BannerPresignedUrlFindResponse issuePresignedUrlForBanner(Long memberId, String bannerImage) {
		validateMemberExists(memberId);
		return BannerPresignedUrlFindResponse.from(fileStoragePort.issuePresignedUrlForBanner(bannerImage));
	}

	public CarouselFindAllResponse findAllPromotionsSortedByCarouselNumber(Long memberId) {
		validateMemberExists(memberId);
		return CarouselFindAllResponse.from(
			toPromotionResults(promotionRepository.findAll())
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

	private void validateMemberExists(Long memberId) {
		memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(AdminApplicationErrorCode.MEMBER_NOT_FOUND));
	}
}
