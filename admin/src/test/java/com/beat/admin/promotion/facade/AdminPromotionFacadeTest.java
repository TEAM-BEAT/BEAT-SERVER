package com.beat.admin.promotion.facade;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.beat.admin.promotion.api.request.CarouselHandleRequest;
import com.beat.admin.promotion.application.command.CarouselHandleCommand;
import com.beat.admin.promotion.application.command.AdminPromotionCommandService;
import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.BannerPresignedUrlResult;
import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult;
import com.beat.admin.promotion.application.result.AdminPromotionResults;
import com.beat.admin.promotion.application.query.AdminPromotionQueryService;

class AdminPromotionFacadeTest {

	@Test
	void facadeDelegatesPromotionScenariosToApplicationServices() {
		AdminPromotionQueryService queryService = mock(AdminPromotionQueryService.class);
		AdminPromotionCommandService commandService = mock(AdminPromotionCommandService.class);
		AdminPromotionFacade adminPromotionFacade = new AdminPromotionFacade(queryService, commandService);
		CarouselHandleRequest request = new CarouselHandleRequest(List.of());
		when(queryService.issueAllPresignedUrlsForCarousel(1L, List.of("carousel.png")))
			.thenReturn(new CarouselPresignedUrlsResult(Map.of()));
		when(queryService.issuePresignedUrlForBanner(1L, "banner.png"))
			.thenReturn(new BannerPresignedUrlResult("banner-url"));
		when(queryService.findAllPromotionsSortedByCarouselNumber(1L))
			.thenReturn(new AdminPromotionResults(List.of()));
		when(commandService.processAllPromotionsSortedByCarouselNumber(1L, new CarouselHandleCommand(List.of())))
			.thenReturn(new AdminPromotionResults(List.of()));

		adminPromotionFacade.checkMemberAndIssueAllPresignedUrlsForCarousel(1L, List.of("carousel.png"));
		adminPromotionFacade.checkMemberAndIssuePresignedUrlForBanner(1L, "banner.png");
		adminPromotionFacade.checkMemberAndFindAllPromotionsSortedByCarouselNumber(1L);
		adminPromotionFacade.checkMemberAndProcessAllPromotionsSortedByCarouselNumber(1L, request);

		verify(queryService).issueAllPresignedUrlsForCarousel(1L, List.of("carousel.png"));
		verify(queryService).issuePresignedUrlForBanner(1L, "banner.png");
		verify(queryService).findAllPromotionsSortedByCarouselNumber(1L);
		verify(commandService).processAllPromotionsSortedByCarouselNumber(1L, new CarouselHandleCommand(List.of()));
	}
}
