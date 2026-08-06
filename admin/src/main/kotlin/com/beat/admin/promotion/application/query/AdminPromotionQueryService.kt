package com.beat.admin.promotion.application.query

import com.beat.admin.exception.AdminApplicationException
import com.beat.admin.promotion.application.AdminPromotionResultAssembler
import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.BannerPresignedUrlResult
import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult
import com.beat.admin.promotion.application.result.AdminPromotionResults
import com.beat.admin.promotion.exception.PromotionApplicationErrorCode
import com.beat.contracts.storage.FileStoragePort
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.promotion.repository.PromotionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
        return AdminPromotionResultAssembler.assemble(promotionRepository.findAll())
    }

    private fun validateMemberExists(memberId: Long) {
        memberRepository.findById(memberId)
            .orElseThrow { AdminApplicationException(PromotionApplicationErrorCode.MEMBER_NOT_FOUND) }
    }
}