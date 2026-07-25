package com.beat.admin.promotion.application.query;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.admin.exception.AdminApplicationException;
import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.BannerPresignedUrlResult;
import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult;
import com.beat.admin.promotion.application.result.AdminPromotionResults;
import com.beat.admin.promotion.application.result.AdminPromotionResults.AdminPromotionResult;
import com.beat.admin.promotion.exception.PromotionApplicationErrorCode;
import com.beat.contracts.storage.FileStoragePort;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.promotion.model.CarouselNumber;
import com.beat.domain.promotion.model.Promotion;
import com.beat.domain.promotion.repository.PromotionRepository;

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

	public CarouselPresignedUrlsResult issueAllPresignedUrlsForCarousel(Long memberId,
		List<String> carouselImages) {
		validateMemberExists(memberId);
		return new CarouselPresignedUrlsResult(
			fileStoragePort.issueAllPresignedUrlsForCarousel(carouselImages).getCarouselPresignedUrls());
	}

	public BannerPresignedUrlResult issuePresignedUrlForBanner(Long memberId, String bannerImage) {
		validateMemberExists(memberId);
		return new BannerPresignedUrlResult(
			fileStoragePort.issuePresignedUrlForBanner(bannerImage).getBannerPresignedUrl());
	}

	public AdminPromotionResults findAllPromotionsSortedByCarouselNumber(Long memberId) {
		validateMemberExists(memberId);
		return toPromotionResults(promotionRepository.findAll());
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

	private void validateMemberExists(Long memberId) {
		memberRepository.findById(memberId)
			.orElseThrow(() -> new AdminApplicationException(PromotionApplicationErrorCode.MEMBER_NOT_FOUND));
	}
}
