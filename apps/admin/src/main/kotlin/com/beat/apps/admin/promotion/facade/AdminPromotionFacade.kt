package com.beat.apps.admin.promotion.facade

import com.beat.application.admin.promotion.command.AdminPromotionCommandService
import com.beat.application.admin.promotion.command.CarouselHandleCommand
import com.beat.application.admin.promotion.command.CarouselHandleCommand.PromotionGenerateCommand
import com.beat.application.admin.promotion.command.CarouselHandleCommand.PromotionModifyCommand
import com.beat.application.admin.promotion.command.PromotionHandleCommand
import com.beat.application.admin.promotion.query.AdminPromotionQueryService
import com.beat.apps.admin.promotion.api.request.CarouselHandleRequest
import com.beat.apps.admin.promotion.api.request.CarouselHandleRequest.PromotionGenerateRequest
import com.beat.apps.admin.promotion.api.request.CarouselHandleRequest.PromotionModifyRequest
import com.beat.apps.admin.promotion.api.request.PromotionHandleRequest
import com.beat.apps.admin.promotion.api.response.BannerPresignedUrlFindResponse
import com.beat.apps.admin.promotion.api.response.CarouselFindAllResponse
import com.beat.apps.admin.promotion.api.response.CarouselHandleAllResponse
import com.beat.apps.admin.promotion.api.response.CarouselPresignedUrlFindAllResponse
import org.springframework.stereotype.Service

@Service
class AdminPromotionFacade(
    private val adminPromotionQueryService: AdminPromotionQueryService,
    private val adminPromotionCommandService: AdminPromotionCommandService,
) {
    fun checkMemberAndIssueAllPresignedUrlsForCarousel(
        memberId: Long,
        carouselImages: List<String>,
    ): CarouselPresignedUrlFindAllResponse =
        CarouselPresignedUrlFindAllResponse(
            adminPromotionQueryService.issueAllPresignedUrlsForCarousel(memberId, carouselImages)
        )

    fun checkMemberAndIssuePresignedUrlForBanner(
        memberId: Long,
        bannerImage: String,
    ): BannerPresignedUrlFindResponse =
        BannerPresignedUrlFindResponse(
            adminPromotionQueryService.issuePresignedUrlForBanner(memberId, bannerImage)
        )

    fun checkMemberAndFindAllPromotionsSortedByCarouselNumber(
        memberId: Long
    ): CarouselFindAllResponse =
        CarouselFindAllResponse(
            adminPromotionQueryService.findAllPromotionsSortedByCarouselNumber(memberId)
        )

    fun checkMemberAndProcessAllPromotionsSortedByCarouselNumber(
        memberId: Long,
        request: CarouselHandleRequest,
    ): CarouselHandleAllResponse =
        CarouselHandleAllResponse(
            adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(
                memberId,
                toCarouselHandleCommand(request),
            )
        )

    private fun toCarouselHandleCommand(request: CarouselHandleRequest): CarouselHandleCommand =
        CarouselHandleCommand(request.carousels.map { toPromotionHandleCommand(it) })

    private fun toPromotionHandleCommand(request: PromotionHandleRequest): PromotionHandleCommand =
        when (request) {
            is PromotionModifyRequest ->
                PromotionModifyCommand(
                    request.promotionId,
                    request.carouselNumber.name,
                    request.newImageUrl,
                    request.isExternal,
                    request.redirectUrl,
                    request.performanceId,
                )
            is PromotionGenerateRequest ->
                PromotionGenerateCommand(
                    request.carouselNumber.name,
                    request.newImageUrl,
                    request.isExternal,
                    request.redirectUrl,
                    request.performanceId,
                )
        }
}
