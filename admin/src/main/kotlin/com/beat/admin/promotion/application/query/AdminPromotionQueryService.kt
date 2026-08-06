package com.beat.admin.promotion.application.query

import com.beat.admin.exception.AdminApplicationException
import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.BannerPresignedUrlResult
import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult
import com.beat.admin.promotion.application.result.AdminPromotionResults
import com.beat.admin.promotion.application.result.AdminPromotionResults.AdminPromotionResult
import com.beat.admin.promotion.exception.PromotionApplicationErrorCode
import com.beat.contracts.storage.FileStoragePort
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.promotion.model.Promotion
import com.beat.domain.promotion.repository.PromotionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val BY_CAROUSEL_NUMBER = compareBy<Promotion> { it.carouselNumber.ordinal }

@Service
@Transactional(readOnly = true)
class AdminPromotionQueryService(
    private val fileStoragePort: FileStoragePort,
    private val memberRepository: MemberRepository,
    private val promotionRepository: PromotionRepository,
) {
    fun issueAllPresignedUrlsForCarousel(memberId: Long, carouselImages: List<String>): CarouselPresignedUrlsResult {
        validateMemberExists(memberId)
        return CarouselPresignedUrlsResult(
            fileStoragePort.issueAllPresignedUrlsForCarousel(carouselImages).carouselPresignedUploads,
        )
    }

    fun issuePresignedUrlForBanner(memberId: Long, bannerImage: String): BannerPresignedUrlResult {
        validateMemberExists(memberId)
        val upload = fileStoragePort.issuePresignedUrlForBanner(bannerImage)
        return BannerPresignedUrlResult(upload.bannerPresignedUrl, upload.bannerImageKey)
    }

    fun findAllPromotionsSortedByCarouselNumber(memberId: Long): AdminPromotionResults {
        validateMemberExists(memberId)
        return toPromotionResults(promotionRepository.findAll())
    }

    private fun toPromotionResults(domainPromotions: List<Promotion>): AdminPromotionResults {
        val promotionResults = domainPromotions.sortedWith(BY_CAROUSEL_NUMBER).map { it.toPromotionResult() }
        return AdminPromotionResults(promotionResults)
    }

    private fun Promotion.toPromotionResult(): AdminPromotionResult = AdminPromotionResult(
        promotionId = getId(),
        carouselNumber = carouselNumber.name,
        newImageUrl = promotionPhoto,
        isExternal = isExternal,
        redirectUrl = redirectUrl,
        performanceId = getPerformanceId(),
    )

    private fun validateMemberExists(memberId: Long) {
        memberRepository.findById(memberId)
            .orElseThrow { AdminApplicationException(PromotionApplicationErrorCode.MEMBER_NOT_FOUND) }
    }
}