package com.beat.admin.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.admin.promotion.application.dto.request.AdminCarouselNumber;
import com.beat.admin.promotion.application.dto.request.CarouselHandleRequest;
import com.beat.admin.promotion.application.dto.request.CarouselHandleRequest.PromotionGenerateRequest;
import com.beat.admin.promotion.application.dto.request.CarouselHandleRequest.PromotionModifyRequest;
import com.beat.admin.promotion.application.dto.request.PromotionHandleRequest;
import com.beat.admin.promotion.application.dto.response.CarouselHandleAllResponse;
import com.beat.admin.promotion.exception.PromotionApplicationErrorCode;
import com.beat.admin.promotion.application.service.command.AdminPromotionCommandService;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.vo.SocialIdentity;
import com.beat.domain.member.domain.SocialType;
import com.beat.contracts.cdn.ImageCachePort;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.performance.vo.PerformancePeriod;
import com.beat.domain.performance.vo.RunningTime;
import com.beat.domain.performance.vo.TicketPrice;
import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.admin.exception.AdminApplicationException;

@ExtendWith(MockitoExtension.class)
class AdminPromotionCommandServiceTest {

	private static final long MEMBER_ID = 7L;
	private static final long PERFORMANCE_ID = 11L;

	@Mock
	private ImageCachePort imageCachePort;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private PromotionRepository promotionRepository;

	@Mock
	private PerformanceRepository performanceRepository;

	@InjectMocks
	private AdminPromotionCommandService adminPromotionCommandService;

	@Captor
	private ArgumentCaptor<List<Long>> deleteIdsCaptor;

	@Test
	void processAllPromotionsRejectsInvalidCarouselItemBeforeMutation() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		CarouselHandleRequest request = new CarouselHandleRequest(
			Collections.<PromotionHandleRequest>singletonList(null)
		);

		AdminApplicationException exception = assertThrows(AdminApplicationException.class, () -> adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, request));

		assertEquals(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT, exception.getErrorCode());
		verify(promotionRepository, never()).findAll();
		verify(promotionRepository, never()).deleteByPromotionIds(any());
		verify(promotionRepository, never()).save(any());
		verifyNoInteractions(performanceRepository);
	}

	@Test
	void processAllPromotionsRejectsInvalidCarouselAssignmentsBeforeMutation() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		CarouselHandleRequest request = new CarouselHandleRequest(List.of(
			new PromotionGenerateRequest(AdminCarouselNumber.ONE, "image-1", true, "url-1", null),
			new PromotionGenerateRequest(AdminCarouselNumber.ONE, "image-2", true, "url-2", null)
		));

		AdminApplicationException exception = assertThrows(AdminApplicationException.class, () -> adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, request));

		assertEquals(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT, exception.getErrorCode());
		verify(promotionRepository, never()).findAll();
		verify(promotionRepository, never()).deleteByPromotionIds(any());
		verify(promotionRepository, never()).save(any());
		verifyNoInteractions(performanceRepository);
	}

	@Test
	void processAllPromotionsRejectsDuplicateModifyIdsBeforeMutation() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		CarouselHandleRequest request = new CarouselHandleRequest(List.of(
			new PromotionModifyRequest(1L, AdminCarouselNumber.ONE, "image-1", true, "url-1", null),
			new PromotionModifyRequest(1L, AdminCarouselNumber.TWO, "image-2", true, "url-2", null)
		));

		AdminApplicationException exception = assertThrows(AdminApplicationException.class, () -> adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, request));

		assertEquals(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT, exception.getErrorCode());
		verify(promotionRepository, never()).findAll();
		verifyNoInteractions(performanceRepository);
	}

	@Test
	void processAllPromotionsPreservesDeletionSaveAndSortedResponseBehavior() {
		Promotion existingPromotion = promotion(1L, "old-image", PERFORMANCE_ID, "old-url", false, CarouselNumber.TWO);
		Promotion omittedPromotion = promotion(2L, "delete-image", null, "delete-url", true, CarouselNumber.FIVE);

		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		when(promotionRepository.findAll()).thenReturn(List.of(existingPromotion, omittedPromotion));
		when(promotionRepository.findById(1L)).thenReturn(Optional.of(existingPromotion));
		when(performanceRepository.findById(PERFORMANCE_ID)).thenReturn(Optional.of(performance()));
		when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> {
			Promotion savedPromotion = invocation.getArgument(0);
			if (savedPromotion.getId() == null) {
				return promotion(3L, savedPromotion.getPromotionPhoto(), savedPromotion.getPerformanceId(),
					savedPromotion.getRedirectUrl(), savedPromotion.isExternal(), savedPromotion.getCarouselNumber());
			}
			return savedPromotion;
		});

		CarouselHandleRequest request = new CarouselHandleRequest(List.of(
			new PromotionModifyRequest(1L, AdminCarouselNumber.THREE, "prod/carousel/modified-image", true, "modified-url",
				PERFORMANCE_ID),
			new PromotionGenerateRequest(AdminCarouselNumber.ONE, "prod/carousel/created-image", false, "created-url", null)
		));

		CarouselHandleAllResponse response =
			adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, request);

		verify(promotionRepository).deleteByPromotionIds(deleteIdsCaptor.capture());
		assertEquals(List.of(2L), deleteIdsCaptor.getValue());
		verify(performanceRepository).findById(PERFORMANCE_ID);

		assertEquals(2, response.modifiedPromotionResponses().size());
		assertEquals(3L, response.modifiedPromotionResponses().get(0).promotionId());
		assertEquals("prod/carousel/created-image", response.modifiedPromotionResponses().get(0).newImageUrl());
		assertEquals("ONE", response.modifiedPromotionResponses().get(0).carouselNumber());
		assertEquals(1L, response.modifiedPromotionResponses().get(1).promotionId());
		assertEquals("prod/carousel/modified-image", response.modifiedPromotionResponses().get(1).newImageUrl());
		assertEquals("THREE", response.modifiedPromotionResponses().get(1).carouselNumber());
	}

	private static Promotion promotion(Long id, String imageUrl, Long performanceId, String redirectUrl,
		boolean isExternal, CarouselNumber carouselNumber) {
		return Promotion.rehydrate(id, imageUrl, performanceId, redirectUrl, isExternal, carouselNumber);
	}

	private static Member member() {
		return Member.rehydrate(MEMBER_ID, "admin", "admin@example.com", null, 1L, SocialIdentity.of(SocialType.KAKAO, 10L));
	}

	private static Performance performance() {
		return Performance.rehydrate(
			PERFORMANCE_ID,
			"title",
			Genre.PLAY,
			RunningTime.of(100),
			"description",
			"attention",
			null,
			"poster",
			"team",
			"venue",
			"road",
			"detail",
			"37.0",
			"127.0",
			"contact",
			PerformancePeriod.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)),
			TicketPrice.of(0),
			1,
			1L
		);
	}
}
