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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.admin.promotion.application.command.CarouselHandleCommand;
import com.beat.admin.promotion.application.command.CarouselHandleCommand.PromotionGenerateCommand;
import com.beat.admin.promotion.application.command.CarouselHandleCommand.PromotionModifyCommand;
import com.beat.admin.promotion.application.command.PromotionHandleCommand;
import com.beat.admin.promotion.application.result.AdminPromotionResults;
import com.beat.admin.promotion.exception.PromotionApplicationErrorCode;
import com.beat.admin.promotion.application.command.AdminPromotionCommandService;
import com.beat.domain.member.model.Member;
import com.beat.domain.member.vo.SocialIdentity;
import com.beat.domain.member.model.SocialType;
import com.beat.contracts.cdn.ImageCachePort;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.performance.model.Genre;
import com.beat.contracts.performance.PerformanceSummaryReadPort;
import com.beat.contracts.performance.readmodel.PerformanceSummaryReadModel;
import com.beat.contracts.storage.FileStoragePort;
import com.beat.contracts.storage.ImageObjectMetadata;
import com.beat.domain.performance.vo.PerformancePeriod;
import com.beat.domain.performance.vo.RunningTime;
import com.beat.domain.performance.vo.TicketPrice;
import com.beat.domain.promotion.model.CarouselNumber;
import com.beat.domain.promotion.model.Promotion;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.domain.promotion.service.PromotionCarouselDomainService;
import com.beat.admin.exception.AdminApplicationException;

@ExtendWith(MockitoExtension.class)
class AdminPromotionCommandServiceTest {

	private static final long MEMBER_ID = 7L;
	private static final long PERFORMANCE_ID = 11L;

	@Mock
	private ImageCachePort imageCachePort;

	@Mock
	private FileStoragePort fileStoragePort;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private PromotionRepository promotionRepository;

	@Mock
	private PerformanceSummaryReadPort performanceSummaryReadPort;

	@Spy
	private PromotionCarouselDomainService promotionCarouselDomainService = new PromotionCarouselDomainService();

	@InjectMocks
	private AdminPromotionCommandService adminPromotionCommandService;

	@Captor
	private ArgumentCaptor<List<Long>> deleteIdsCaptor;

	@Test
	void processAllPromotionsRejectsInvalidCarouselItemBeforeMutation() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		CarouselHandleCommand command = CarouselHandleCommand.from(
			Collections.<PromotionHandleCommand>singletonList(null)
		);

		AdminApplicationException exception = assertThrows(AdminApplicationException.class, () -> adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command));

		assertEquals(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT, exception.getErrorCode());
		verify(promotionRepository, never()).findAll();
		verify(promotionRepository, never()).deleteByPromotionIds(any());
		verify(promotionRepository, never()).save(any());
		verifyNoInteractions(performanceSummaryReadPort);
	}

	@Test
	void processAllPromotionsRejectsInvalidCarouselAssignmentsBeforeMutation() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		CarouselHandleCommand command = CarouselHandleCommand.from(List.of(
			PromotionGenerateCommand.of("ONE", "image-1", true, "url-1", null),
			PromotionGenerateCommand.of("ONE", "image-2", true, "url-2", null)
		));

		AdminApplicationException exception = assertThrows(AdminApplicationException.class, () -> adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command));

		assertEquals(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT, exception.getErrorCode());
		verify(promotionRepository, never()).findAll();
		verify(promotionRepository, never()).deleteByPromotionIds(any());
		verify(promotionRepository, never()).save(any());
		verifyNoInteractions(performanceSummaryReadPort);
	}

	@Test
	void processAllPromotionsRejectsDuplicateModifyIdsBeforeMutation() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		CarouselHandleCommand command = CarouselHandleCommand.from(List.of(
			PromotionModifyCommand.of(1L, "ONE", "image-1", true, "url-1", null),
			PromotionModifyCommand.of(1L, "TWO", "image-2", true, "url-2", null)
		));

		AdminApplicationException exception = assertThrows(AdminApplicationException.class, () -> adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command));

		assertEquals(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT, exception.getErrorCode());
		verify(promotionRepository, never()).findAll();
		verifyNoInteractions(performanceSummaryReadPort);
	}

	@Test
	void processAllPromotionsPreservesDeletionSaveAndSortedResponseBehavior() {
		Promotion existingPromotion = promotion(1L, "old-image", PERFORMANCE_ID, "old-url", false, CarouselNumber.TWO);
		Promotion omittedPromotion = promotion(2L, "delete-image", null, "delete-url", true, CarouselNumber.FIVE);

		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		when(promotionRepository.findAll()).thenReturn(List.of(existingPromotion, omittedPromotion));
		when(promotionRepository.findById(1L)).thenReturn(Optional.of(existingPromotion));
		when(performanceSummaryReadPort.findById(PERFORMANCE_ID)).thenReturn(Optional.of(performance()));
		when(fileStoragePort.findCarouselImageObjectMetadata("prod/carousel/modified-image"))
			.thenReturn(validImage());
		when(fileStoragePort.findCarouselImageObjectMetadata("prod/carousel/created-image"))
			.thenReturn(validImage());
		when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> {
			Promotion savedPromotion = invocation.getArgument(0);
			if (savedPromotion.getId() == null) {
				return promotion(3L, savedPromotion.getPromotionPhoto(), savedPromotion.getPerformanceId(),
					savedPromotion.getRedirectUrl(), savedPromotion.isExternal(), savedPromotion.getCarouselNumber());
			}
			return savedPromotion;
		});

		CarouselHandleCommand command = CarouselHandleCommand.from(List.of(
			PromotionModifyCommand.of(1L, "THREE", "prod/carousel/modified-image", true, "modified-url",
				PERFORMANCE_ID),
			PromotionGenerateCommand.of("ONE", "prod/carousel/created-image", false, "created-url", null)
		));

		AdminPromotionResults response =
			adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command);

		verify(promotionRepository).deleteByPromotionIds(deleteIdsCaptor.capture());
		assertEquals(List.of(2L), deleteIdsCaptor.getValue());
		verify(performanceSummaryReadPort).findById(PERFORMANCE_ID);

		assertEquals(2, response.promotionResults().size());
		assertEquals(3L, response.promotionResults().get(0).promotionId());
		assertEquals("prod/carousel/created-image", response.promotionResults().get(0).newImageUrl());
		assertEquals("ONE", response.promotionResults().get(0).carouselNumber());
		assertEquals(1L, response.promotionResults().get(1).promotionId());
		assertEquals("prod/carousel/modified-image", response.promotionResults().get(1).newImageUrl());
		assertEquals("THREE", response.promotionResults().get(1).carouselNumber());
	}

	@Test
	void processAllPromotionsRejectsMissingUploadedImageBeforeMutation() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		when(fileStoragePort.findCarouselImageObjectMetadata("prod/carousel/missing-image")).thenReturn(null);

		CarouselHandleCommand command = CarouselHandleCommand.from(List.of(
			PromotionGenerateCommand.of("ONE", "prod/carousel/missing-image", false, "created-url", null)
		));

		AdminApplicationException exception = assertThrows(AdminApplicationException.class,
			() -> adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command));

		assertEquals(PromotionApplicationErrorCode.INVALID_IMAGE_UPLOAD, exception.getErrorCode());
		verify(promotionRepository, never()).findAll();
		verify(promotionRepository, never()).deleteByPromotionIds(any());
		verify(promotionRepository, never()).save(any());
	}

	@Test
	void processAllPromotionsRejectsUnsupportedImageMetadataBeforeMutation() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		when(fileStoragePort.findCarouselImageObjectMetadata("prod/carousel/invalid-image"))
			.thenReturn(ImageObjectMetadata.of("application/pdf", 1024L));

		CarouselHandleCommand command = CarouselHandleCommand.from(List.of(
			PromotionGenerateCommand.of("ONE", "prod/carousel/invalid-image", false, "created-url", null)
		));

		AdminApplicationException exception = assertThrows(AdminApplicationException.class,
			() -> adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command));

		assertEquals(PromotionApplicationErrorCode.INVALID_IMAGE_UPLOAD, exception.getErrorCode());
		verify(promotionRepository, never()).findAll();
	}

	@Test
	void processAllPromotionsRejectsOversizedImageBeforeMutation() {
		when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()));
		when(fileStoragePort.findCarouselImageObjectMetadata("prod/carousel/oversized-image"))
			.thenReturn(ImageObjectMetadata.of("image/png", 11L * 1024 * 1024));

		CarouselHandleCommand command = CarouselHandleCommand.from(List.of(
			PromotionGenerateCommand.of("ONE", "prod/carousel/oversized-image", false, "created-url", null)
		));

		AdminApplicationException exception = assertThrows(AdminApplicationException.class,
			() -> adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command));

		assertEquals(PromotionApplicationErrorCode.INVALID_IMAGE_UPLOAD, exception.getErrorCode());
		verify(promotionRepository, never()).findAll();
	}

	private static ImageObjectMetadata validImage() {
		return ImageObjectMetadata.of("image/png", 1024L);
	}

	private static Promotion promotion(Long id, String imageUrl, Long performanceId, String redirectUrl,
		boolean isExternal, CarouselNumber carouselNumber) {
		return Promotion.rehydrate(id, imageUrl, performanceId, redirectUrl, isExternal, carouselNumber);
	}

	private static Member member() {
		return Member.rehydrate(MEMBER_ID, "admin", "admin@example.com", null, 1L, SocialIdentity.of(SocialType.KAKAO, 10L));
	}

	private static PerformanceSummaryReadModel performance() {
		return new PerformanceSummaryReadModel(
			PERFORMANCE_ID,
			1L,
			"title",
			"PLAY",
			0,
			null,
			null,
			null,
			"poster",
			"team",
			"venue",
			"contact",
			1,
			LocalDate.of(2026, 1, 1),
			LocalDate.of(2026, 1, 1)
		);
	}
}
