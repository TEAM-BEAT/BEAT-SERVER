package com.beat.admin.promotion.facade

import com.beat.admin.promotion.api.request.CarouselHandleRequest
import com.beat.admin.promotion.application.command.AdminPromotionCommandService
import com.beat.admin.promotion.application.command.CarouselHandleCommand
import com.beat.admin.promotion.application.query.AdminPromotionQueryService
import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.BannerPresignedUrlResult
import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult
import com.beat.admin.promotion.application.result.AdminPromotionResults
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class AdminPromotionFacadeTest {

    @Test
    fun facadeDelegatesPromotionScenariosToApplicationServices() {
        val queryService = mock(AdminPromotionQueryService::class.java)
        val commandService = mock(AdminPromotionCommandService::class.java)
        val adminPromotionFacade = AdminPromotionFacade(queryService, commandService)
        val request = CarouselHandleRequest(emptyList())
        `when`(queryService.issueAllPresignedUrlsForCarousel(1L, listOf("carousel.png")))
            .thenReturn(CarouselPresignedUrlsResult(emptyMap()))
        `when`(queryService.issuePresignedUrlForBanner(1L, "banner.png"))
            .thenReturn(BannerPresignedUrlResult("banner-url", "prod/banner/banner.png"))
        `when`(queryService.findAllPromotionsSortedByCarouselNumber(1L))
            .thenReturn(AdminPromotionResults(emptyList()))
        `when`(commandService.processAllPromotionsSortedByCarouselNumber(1L, CarouselHandleCommand.from(emptyList())))
            .thenReturn(AdminPromotionResults(emptyList()))

        adminPromotionFacade.checkMemberAndIssueAllPresignedUrlsForCarousel(1L, listOf("carousel.png"))
        adminPromotionFacade.checkMemberAndIssuePresignedUrlForBanner(1L, "banner.png")
        adminPromotionFacade.checkMemberAndFindAllPromotionsSortedByCarouselNumber(1L)
        adminPromotionFacade.checkMemberAndProcessAllPromotionsSortedByCarouselNumber(1L, request)

        verify(queryService).issueAllPresignedUrlsForCarousel(1L, listOf("carousel.png"))
        verify(queryService).issuePresignedUrlForBanner(1L, "banner.png")
        verify(queryService).findAllPromotionsSortedByCarouselNumber(1L)
        verify(commandService).processAllPromotionsSortedByCarouselNumber(1L, CarouselHandleCommand.from(emptyList()))
    }
}