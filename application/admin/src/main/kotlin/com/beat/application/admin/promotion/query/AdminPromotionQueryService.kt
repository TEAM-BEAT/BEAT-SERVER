package com.beat.application.admin.promotion.query

import com.beat.application.admin.exception.AdminApplicationException
import com.beat.application.admin.exception.translateDomainFailure
import com.beat.application.admin.promotion.AdminPromotionResultAssembler
import com.beat.application.admin.promotion.AdminPromotionResults
import com.beat.application.admin.promotion.PromotionImageStorage
import com.beat.application.admin.promotion.exception.PromotionApplicationErrorCode
import com.beat.application.admin.promotion.query.AdminPromotionPresignedUrlResults.BannerPresignedUrlResult
import com.beat.application.admin.promotion.query.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.promotion.repository.PromotionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminPromotionQueryService internal constructor(
    private val promotionImageStorage: PromotionImageStorage,
    private val memberRepository: MemberRepository,
    private val promotionRepository: PromotionRepository,
) {
    fun issueAllPresignedUrlsForCarousel(memberId: Long, carouselImages: List<String>): CarouselPresignedUrlsResult {
        return translateDomainFailure {
            validateMemberExists(memberId)
            CarouselPresignedUrlsResult(
                promotionImageStorage.issueCarouselUploads(carouselImages),
            )
        }
    }

    fun issuePresignedUrlForBanner(memberId: Long, bannerImage: String): BannerPresignedUrlResult {
        return translateDomainFailure {
            validateMemberExists(memberId)
            val upload = promotionImageStorage.issueBannerUpload(bannerImage)
            BannerPresignedUrlResult(upload.uploadUrl, upload.imageKey)
        }
    }

    fun findAllPromotionsSortedByCarouselNumber(memberId: Long): AdminPromotionResults {
        return translateDomainFailure {
            validateMemberExists(memberId)
            AdminPromotionResultAssembler.assemble(promotionRepository.findAll())
        }
    }

    private fun validateMemberExists(memberId: Long) {
        memberRepository.findById(memberId)
            ?: throw AdminApplicationException(PromotionApplicationErrorCode.MEMBER_NOT_FOUND)
    }
}
