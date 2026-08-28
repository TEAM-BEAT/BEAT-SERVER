package com.beat.application.admin.promotion.command

import com.beat.application.admin.exception.AdminApplicationException
import com.beat.application.admin.exception.translateDomainFailure
import com.beat.application.admin.promotion.exception.PromotionApplicationErrorCode
import com.beat.domain.member.repository.MemberRepository
import org.springframework.stereotype.Service

@Service
class PromotionImageCommandService
internal constructor(
    private val promotionImageStorage: PromotionImageStorage,
    private val memberRepository: MemberRepository,
) {
    fun issueAllPresignedUrlsForCarousel(
        memberId: Long,
        carouselImages: List<String>,
    ): AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult {
        return translateDomainFailure {
            validateMemberExists(memberId)
            AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult(
                promotionImageStorage.issueCarouselUploads(carouselImages)
            )
        }
    }

    fun issuePresignedUrlForBanner(
        memberId: Long,
        bannerImage: String,
    ): AdminPromotionPresignedUrlResults.BannerPresignedUrlResult {
        return translateDomainFailure {
            validateMemberExists(memberId)
            val upload = promotionImageStorage.issueBannerUpload(bannerImage)
            AdminPromotionPresignedUrlResults.BannerPresignedUrlResult(
                upload.uploadUrl,
                upload.imageKey,
            )
        }
    }

    private fun validateMemberExists(memberId: Long) {
        memberRepository.findById(memberId)
            ?: throw AdminApplicationException(PromotionApplicationErrorCode.MEMBER_NOT_FOUND)
    }
}
