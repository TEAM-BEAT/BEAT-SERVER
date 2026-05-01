package com.beat.admin.facade;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.beat.admin.application.AdminCommandService;
import com.beat.admin.application.AdminQueryService;
import com.beat.admin.application.dto.request.CarouselHandleRequest;

class AdminFacadeTest {

	@Test
	void facadeDelegatesAdminScenariosToApplicationServices() {
		AdminQueryService queryService = mock(AdminQueryService.class);
		AdminCommandService commandService = mock(AdminCommandService.class);
		AdminFacade adminFacade = new AdminFacade(queryService, commandService);
		CarouselHandleRequest request = new CarouselHandleRequest(List.of());

		adminFacade.checkMemberAndFindAllUsers(1L);
		adminFacade.checkMemberAndIssueAllPresignedUrlsForCarousel(1L, List.of("carousel.png"));
		adminFacade.checkMemberAndIssuePresignedUrlForBanner(1L, "banner.png");
		adminFacade.checkMemberAndFindAllPromotionsSortedByCarouselNumber(1L);
		adminFacade.checkMemberAndProcessAllPromotionsSortedByCarouselNumber(1L, request);

		verify(queryService).findAllUsers(1L);
		verify(queryService).issueAllPresignedUrlsForCarousel(1L, List.of("carousel.png"));
		verify(queryService).issuePresignedUrlForBanner(1L, "banner.png");
		verify(queryService).findAllPromotionsSortedByCarouselNumber(1L);
		verify(commandService).processAllPromotionsSortedByCarouselNumber(1L, request);
	}
}
