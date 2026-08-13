package com.beat.admin.promotion.api

import com.beat.admin.promotion.api.request.CarouselHandleRequest
import com.beat.admin.promotion.api.response.BannerPresignedUrlFindResponse
import com.beat.admin.promotion.api.response.CarouselFindAllResponse
import com.beat.admin.promotion.api.response.CarouselHandleAllResponse
import com.beat.admin.promotion.api.response.CarouselPresignedUrlFindAllResponse
import com.beat.admin.promotion.api.response.PromotionSuccessCode
import com.beat.admin.promotion.facade.AdminPromotionFacade
import com.beat.gateway.CurrentMember
import com.beat.global.support.response.SuccessResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class AdminPromotionController(
    private val adminPromotionFacade: AdminPromotionFacade,
) : AdminPromotionApi {

    @GetMapping("/carousels/presigned-url")
    override fun createAllCarouselPresignedUrls(
        @CurrentMember memberId: Long,
        @RequestParam carouselImages: List<String>,
    ): ResponseEntity<SuccessResponse<CarouselPresignedUrlFindAllResponse>> {
        val response = adminPromotionFacade.checkMemberAndIssueAllPresignedUrlsForCarousel(memberId, carouselImages)
        return ResponseEntity.ok(SuccessResponse.of(PromotionSuccessCode.CAROUSEL_PRESIGNED_URL_ISSUED, response))
    }

    @GetMapping("/banner/presigned-url")
    override fun createBannerPresignedUrl(
        @CurrentMember memberId: Long,
        @RequestParam bannerImage: String,
    ): ResponseEntity<SuccessResponse<BannerPresignedUrlFindResponse>> {
        val response = adminPromotionFacade.checkMemberAndIssuePresignedUrlForBanner(memberId, bannerImage)
        return ResponseEntity.status(HttpStatus.OK)
            .body(SuccessResponse.of(PromotionSuccessCode.BANNER_PRESIGNED_URL_ISSUED, response))
    }

    @GetMapping("/carousels")
    override fun readAllCarouselImages(
        @CurrentMember memberId: Long,
    ): ResponseEntity<SuccessResponse<CarouselFindAllResponse>> {
        val response = adminPromotionFacade.checkMemberAndFindAllPromotionsSortedByCarouselNumber(memberId)
        return ResponseEntity.status(HttpStatus.OK)
            .body(SuccessResponse.of(PromotionSuccessCode.FETCH_ALL_CAROUSEL_PROMOTIONS_SUCCESS, response))
    }

    @PutMapping("/carousels")
    override fun processCarouselImages(
        @CurrentMember memberId: Long,
        @Valid @RequestBody request: CarouselHandleRequest,
    ): ResponseEntity<SuccessResponse<CarouselHandleAllResponse>> {
        val response = adminPromotionFacade.checkMemberAndProcessAllPromotionsSortedByCarouselNumber(memberId, request)
        return ResponseEntity.status(HttpStatus.OK)
            .body(SuccessResponse.of(PromotionSuccessCode.UPDATE_ALL_CAROUSEL_PROMOTIONS_SUCCESS, response))
    }
}