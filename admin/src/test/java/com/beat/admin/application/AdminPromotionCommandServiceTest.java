package com.beat.admin.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.admin.promotion.application.dto.request.AdminCarouselNumber;
import com.beat.admin.promotion.application.dto.request.CarouselHandleRequest;
import com.beat.admin.promotion.application.dto.request.CarouselHandleRequest.PromotionGenerateRequest;
import com.beat.admin.promotion.application.dto.request.CarouselHandleRequest.PromotionModifyRequest;
import com.beat.admin.promotion.application.dto.request.PromotionHandleRequest;
import com.beat.admin.promotion.application.dto.response.CarouselHandleAllResponse;
import com.beat.admin.application.exception.AdminApplicationErrorCode;
import com.beat.admin.promotion.application.service.command.AdminPromotionCommandService;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;
import com.beat.contracts.cdn.ImageCachePort;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.global.support.exception.BadRequestException;

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

	@Test
	void processAllPromotionsRejectsInvalidCarouselItemBeforeMutation() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		CarouselHandleRequest request = new CarouselHandleRequest(
			Collections.<PromotionHandleRequest>singletonList(null)
		);

		BadRequestException exception = assertThrows(BadRequestException.class,
			() -> adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, request));

		assertEquals(AdminApplicationErrorCode.INVALID_REQUEST_FORMAT, exception.getBaseErrorCode());
		verify(promotionRepository, never()).findAll();
		verify(promotionRepository, never()).deleteByPromotionIds(any());
		verify(promotionRepository, never()).save(any());
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
			new PromotionModifyRequest(1L, AdminCarouselNumber.THREE, "carousel/modified-image", true, "modified-url",
				PERFORMANCE_ID),
			new PromotionGenerateRequest(AdminCarouselNumber.ONE, "carousel/created-image", false, "created-url", null)
		));

		CarouselHandleAllResponse response =
			adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Long>> deleteIdsCaptor = ArgumentCaptor.forClass(List.class);
		verify(promotionRepository).deleteByPromotionIds(deleteIdsCaptor.capture());
		assertEquals(List.of(2L), deleteIdsCaptor.getValue());
		verify(performanceRepository).findById(PERFORMANCE_ID);

		assertEquals(2, response.modifiedPromotionResponses().size());
		assertEquals(3L, response.modifiedPromotionResponses().get(0).promotionId());
		assertEquals("carousel/created-image", response.modifiedPromotionResponses().get(0).newImageUrl());
		assertEquals("ONE", response.modifiedPromotionResponses().get(0).carouselNumber());
		assertEquals(1L, response.modifiedPromotionResponses().get(1).promotionId());
		assertEquals("carousel/modified-image", response.modifiedPromotionResponses().get(1).newImageUrl());
		assertEquals("THREE", response.modifiedPromotionResponses().get(1).carouselNumber());
	}

	private static Promotion promotion(Long id, String imageUrl, Long performanceId, String redirectUrl,
		boolean isExternal, CarouselNumber carouselNumber) {
		return Promotion.rehydrate(id, imageUrl, performanceId, redirectUrl, isExternal, carouselNumber);
	}

	private static Member member() {
		return Member.rehydrate(MEMBER_ID, "admin", "admin@example.com", null, 1L, 10L, SocialType.KAKAO);
	}

	private static Performance performance() {
		return Performance.rehydrate(
			PERFORMANCE_ID,
			"title",
			Genre.PLAY,
			100,
			"description",
			"attention",
			BankName.NONE,
			null,
			null,
			"poster",
			"team",
			"venue",
			"road",
			"detail",
			"37.0",
			"127.0",
			"contact",
			"2026.01.01",
			0,
			1,
			1L
		);
	}
}
