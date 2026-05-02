package com.beat.admin.promotion.facade;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.beat.admin.promotion.application.dto.request.CarouselHandleRequest;
import com.beat.admin.promotion.application.service.command.AdminPromotionCommandService;
import com.beat.admin.promotion.application.service.query.AdminPromotionQueryService;

class AdminPromotionFacadeTest {

	@Test
	void facadeDelegatesPromotionScenariosToApplicationServices() {
		AdminPromotionQueryService queryService = mock(AdminPromotionQueryService.class);
		AdminPromotionCommandService commandService = mock(AdminPromotionCommandService.class);
		AdminPromotionFacade adminPromotionFacade = new AdminPromotionFacade(queryService, commandService);
		CarouselHandleRequest request = new CarouselHandleRequest(List.of());

		adminPromotionFacade.checkMemberAndIssueAllPresignedUrlsForCarousel(1L, List.of("carousel.png"));
		adminPromotionFacade.checkMemberAndIssuePresignedUrlForBanner(1L, "banner.png");
		adminPromotionFacade.checkMemberAndFindAllPromotionsSortedByCarouselNumber(1L);
		adminPromotionFacade.checkMemberAndProcessAllPromotionsSortedByCarouselNumber(1L, request);

		verify(queryService).issueAllPresignedUrlsForCarousel(1L, List.of("carousel.png"));
		verify(queryService).issuePresignedUrlForBanner(1L, "banner.png");
		verify(queryService).findAllPromotionsSortedByCarouselNumber(1L);
		verify(commandService).processAllPromotionsSortedByCarouselNumber(1L, request);
	}
}
