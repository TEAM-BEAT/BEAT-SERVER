package com.beat.admin.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.BannerPresignedUrlResult;
import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult;
import com.beat.admin.promotion.application.result.AdminPromotionResults;
import com.beat.admin.promotion.application.query.AdminPromotionQueryService;
import com.beat.contracts.storage.BannerPresignedUrl;
import com.beat.contracts.storage.CarouselPresignedUpload;
import com.beat.contracts.storage.CarouselPresignedUrls;
import com.beat.contracts.storage.FileStoragePort;
import com.beat.domain.member.model.Member;
import com.beat.domain.member.vo.SocialIdentity;
import com.beat.domain.member.model.SocialType;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.promotion.model.CarouselNumber;
import com.beat.domain.promotion.model.Promotion;
import com.beat.domain.promotion.repository.PromotionRepository;

@ExtendWith(MockitoExtension.class)
class AdminPromotionQueryServiceTest {

	private static final long MEMBER_ID = 7L;

	@Mock
	private FileStoragePort fileStoragePort;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private PromotionRepository promotionRepository;

	@InjectMocks
	private AdminPromotionQueryService adminPromotionQueryService;

	@Test
	void findAllPromotionsPreservesCarouselSortingAndResponseShape() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		when(promotionRepository.findAll()).thenReturn(List.of(
			promotion(2L, "image-two", null, "url-two", true, CarouselNumber.TWO),
			promotion(1L, "image-one", 11L, "url-one", false, CarouselNumber.ONE)
		));

		AdminPromotionResults response = adminPromotionQueryService.findAllPromotionsSortedByCarouselNumber(MEMBER_ID);

		assertEquals(2, response.promotionResults().size());
		assertEquals(1L, response.promotionResults().get(0).promotionId());
		assertEquals("ONE", response.promotionResults().get(0).carouselNumber());
		assertEquals("image-one", response.promotionResults().get(0).newImageUrl());
		assertEquals(11L, response.promotionResults().get(0).performanceId());
		assertEquals(2L, response.promotionResults().get(1).promotionId());
		assertEquals("TWO", response.promotionResults().get(1).carouselNumber());
		assertEquals("image-two", response.promotionResults().get(1).newImageUrl());
	}

	@Test
	void presignedUrlQueriesStillValidateMemberAndDelegateToStoragePort() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		when(fileStoragePort.issueAllPresignedUrlsForCarousel(List.of("carousel.png")))
			.thenReturn(new CarouselPresignedUrls(Map.of("carousel.png",
				CarouselPresignedUpload.of("carousel-upload-url", "dev/carousel/carousel.png"))));
		when(fileStoragePort.issuePresignedUrlForBanner("banner.png"))
			.thenReturn(new BannerPresignedUrl("banner-url", "prod/banner/banner.png"));

		CarouselPresignedUrlsResult carouselResponse =
			adminPromotionQueryService.issueAllPresignedUrlsForCarousel(MEMBER_ID, List.of("carousel.png"));
		BannerPresignedUrlResult bannerResponse =
			adminPromotionQueryService.issuePresignedUrlForBanner(MEMBER_ID, "banner.png");

		assertEquals(Map.of("carousel.png",
			CarouselPresignedUpload.of("carousel-upload-url", "dev/carousel/carousel.png")),
			carouselResponse.carouselPresignedUploads());
		assertEquals("banner-url", bannerResponse.bannerPresignedUrl());
		assertEquals("prod/banner/banner.png", bannerResponse.bannerImageKey());
		verify(fileStoragePort).issueAllPresignedUrlsForCarousel(List.of("carousel.png"));
		verify(fileStoragePort).issuePresignedUrlForBanner("banner.png");
	}

	private static Promotion promotion(Long id, String imageUrl, Long performanceId, String redirectUrl,
		boolean isExternal, CarouselNumber carouselNumber) {
		return Promotion.rehydrate(id, imageUrl, performanceId, redirectUrl, isExternal, carouselNumber);
	}

	private static Member member() {
		return Member.rehydrate(MEMBER_ID, "admin", "admin@example.com", null, 1L, SocialIdentity.of(SocialType.KAKAO, 10L));
	}
}
