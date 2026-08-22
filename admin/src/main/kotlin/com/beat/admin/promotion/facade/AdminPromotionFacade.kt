package com.beat.admin.promotion.facade

import com.beat.admin.promotion.api.request.CarouselHandleRequest
import com.beat.admin.promotion.api.request.CarouselHandleRequest.PromotionGenerateRequest
import com.beat.admin.promotion.api.request.CarouselHandleRequest.PromotionModifyRequest
import com.beat.admin.promotion.api.request.PromotionHandleRequest
import com.beat.admin.promotion.api.response.BannerPresignedUrlFindResponse
import com.beat.admin.promotion.api.response.CarouselFindAllResponse
import com.beat.admin.promotion.api.response.CarouselHandleAllResponse
import com.beat.admin.promotion.api.response.CarouselPresignedUrlFindAllResponse
import com.beat.application.admin.exception.AdminApplicationException
import com.beat.application.admin.promotion.command.AdminPromotionCommandService
import com.beat.application.admin.promotion.command.CarouselHandleCommand
import com.beat.application.admin.promotion.command.CarouselHandleCommand.PromotionGenerateCommand
import com.beat.application.admin.promotion.command.CarouselHandleCommand.PromotionModifyCommand
import com.beat.application.admin.promotion.command.PromotionHandleCommand
import com.beat.application.admin.promotion.exception.PromotionApplicationErrorCode
import com.beat.application.admin.promotion.query.AdminPromotionQueryService
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
            adminPromotionQueryService.issueAllPresignedUrlsForCarousel(memberId, carouselImages),
        )

    fun checkMemberAndIssuePresignedUrlForBanner(memberId: Long, bannerImage: String): BannerPresignedUrlFindResponse =
        BannerPresignedUrlFindResponse(adminPromotionQueryService.issuePresignedUrlForBanner(memberId, bannerImage))

    fun checkMemberAndFindAllPromotionsSortedByCarouselNumber(memberId: Long): CarouselFindAllResponse =
        CarouselFindAllResponse(adminPromotionQueryService.findAllPromotionsSortedByCarouselNumber(memberId))

    fun checkMemberAndProcessAllPromotionsSortedByCarouselNumber(
        memberId: Long,
        request: CarouselHandleRequest,
    ): CarouselHandleAllResponse =
        CarouselHandleAllResponse(
            adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(
                memberId,
                toCarouselHandleCommand(request),
            ),
        )

    private fun toCarouselHandleCommand(request: CarouselHandleRequest): CarouselHandleCommand {
        val carousels = request.carousels ?: throw invalidRequest()
        return CarouselHandleCommand(carousels.map { toPromotionHandleCommand(it) })
    }

    private fun toPromotionHandleCommand(request: PromotionHandleRequest?): PromotionHandleCommand = when (request) {
        is PromotionModifyRequest -> PromotionModifyCommand(
            requireField(request.promotionId),
            requireField(request.carouselNumber).name,
            requireField(request.newImageUrl),
            requireField(request.isExternal),
            requireField(request.redirectUrl),
            request.performanceId,
        )
        is PromotionGenerateRequest -> PromotionGenerateCommand(
            requireField(request.carouselNumber).name,
            requireField(request.newImageUrl),
            requireField(request.isExternal),
            requireField(request.redirectUrl),
            request.performanceId,
        )
        else -> throw invalidRequest()
    }

    private fun <T> requireField(value: T?): T = value ?: throw invalidRequest()

    private fun invalidRequest(): AdminApplicationException =
        AdminApplicationException(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT)
}
