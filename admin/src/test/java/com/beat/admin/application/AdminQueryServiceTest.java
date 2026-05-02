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

import com.beat.admin.application.dto.response.BannerPresignedUrlFindResponse;
import com.beat.admin.application.dto.response.CarouselFindAllResponse;
import com.beat.admin.application.dto.response.CarouselPresignedUrlFindAllResponse;
import com.beat.admin.application.dto.response.UserFindAllResponse;
import com.beat.admin.application.service.query.AdminQueryService;
import com.beat.contracts.storage.BannerPresignedUrl;
import com.beat.contracts.storage.CarouselPresignedUrls;
import com.beat.contracts.storage.FileStoragePort;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.domain.user.domain.Role;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminQueryServiceTest {

	private static final long MEMBER_ID = 7L;

	@Mock
	private FileStoragePort fileStoragePort;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PromotionRepository promotionRepository;

	@InjectMocks
	private AdminQueryService adminQueryService;

	@Test
	void findAllUsersPreservesUserResponseShape() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		when(userRepository.findAll()).thenReturn(List.of(
			Users.rehydrate(1L, Role.USER),
			Users.rehydrate(2L, Role.ADMIN)
		));

		UserFindAllResponse response = adminQueryService.findAllUsers(MEMBER_ID);

		assertEquals(2, response.userResponses().size());
		assertEquals(1L, response.userResponses().get(0).id());
		assertEquals("ROLE_USER", response.userResponses().get(0).role());
		assertEquals(2L, response.userResponses().get(1).id());
		assertEquals("ROLE_ADMIN", response.userResponses().get(1).role());
	}

	@Test
	void findAllPromotionsPreservesCarouselSortingAndResponseShape() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		when(promotionRepository.findAll()).thenReturn(List.of(
			promotion(2L, "image-two", null, "url-two", true, CarouselNumber.TWO),
			promotion(1L, "image-one", 11L, "url-one", false, CarouselNumber.ONE)
		));

		CarouselFindAllResponse response = adminQueryService.findAllPromotionsSortedByCarouselNumber(MEMBER_ID);

		assertEquals(2, response.carouselResponses().size());
		assertEquals(1L, response.carouselResponses().get(0).promotionId());
		assertEquals("ONE", response.carouselResponses().get(0).carouselNumber());
		assertEquals("image-one", response.carouselResponses().get(0).newImageUrl());
		assertEquals(11L, response.carouselResponses().get(0).performanceId());
		assertEquals(2L, response.carouselResponses().get(1).promotionId());
		assertEquals("TWO", response.carouselResponses().get(1).carouselNumber());
		assertEquals("image-two", response.carouselResponses().get(1).newImageUrl());
	}

	@Test
	void presignedUrlQueriesStillValidateMemberAndDelegateToStoragePort() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		when(fileStoragePort.issueAllPresignedUrlsForCarousel(List.of("carousel.png")))
			.thenReturn(new CarouselPresignedUrls(Map.of("carousel.png", "carousel-url")));
		when(fileStoragePort.issuePresignedUrlForBanner("banner.png"))
			.thenReturn(new BannerPresignedUrl("banner-url"));

		CarouselPresignedUrlFindAllResponse carouselResponse =
			adminQueryService.issueAllPresignedUrlsForCarousel(MEMBER_ID, List.of("carousel.png"));
		BannerPresignedUrlFindResponse bannerResponse =
			adminQueryService.issuePresignedUrlForBanner(MEMBER_ID, "banner.png");

		assertEquals(Map.of("carousel.png", "carousel-url"), carouselResponse.carouselPresignedUrls());
		assertEquals("banner-url", bannerResponse.bannerPresignedUrl());
		verify(fileStoragePort).issueAllPresignedUrlsForCarousel(List.of("carousel.png"));
		verify(fileStoragePort).issuePresignedUrlForBanner("banner.png");
	}

	private static Promotion promotion(Long id, String imageUrl, Long performanceId, String redirectUrl,
		boolean isExternal, CarouselNumber carouselNumber) {
		return Promotion.rehydrate(id, imageUrl, performanceId, redirectUrl, isExternal, carouselNumber);
	}

	private static Member member() {
		return Member.rehydrate(MEMBER_ID, "admin", "admin@example.com", null, 1L, 10L, SocialType.KAKAO);
	}
}
