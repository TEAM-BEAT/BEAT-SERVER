package com.beat.domain.promotion.exception

import com.beat.domain.exception.DomainErrorCode
import com.beat.domain.exception.DomainErrorType

enum class PromotionErrorCode(
    override val code: String,
    override val type: DomainErrorType,
    override val message: String,
) : DomainErrorCode {
    TOO_MANY_CAROUSEL_PROMOTIONS(
        "PROMOTION_TOO_MANY_CAROUSEL_PROMOTIONS",
        DomainErrorType.INVALID_INPUT,
        "노출 가능한 캐러셀 프로모션 수를 초과했습니다.",
    ),
    ;
}
