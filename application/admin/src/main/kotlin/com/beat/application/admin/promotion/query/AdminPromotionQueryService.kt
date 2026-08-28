package com.beat.application.admin.promotion.query

import com.beat.application.admin.exception.AdminApplicationException
import com.beat.application.admin.exception.translateDomainFailure
import com.beat.application.admin.promotion.AdminPromotionResultAssembler
import com.beat.application.admin.promotion.AdminPromotionResults
import com.beat.application.admin.promotion.exception.PromotionApplicationErrorCode
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.promotion.repository.PromotionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminPromotionQueryService
internal constructor(
    private val memberRepository: MemberRepository,
    private val promotionRepository: PromotionRepository,
) {
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
