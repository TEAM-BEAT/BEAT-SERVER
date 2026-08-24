package com.beat.application.frontoffice.performance.exception

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType

enum class StaffApplicationErrorCode(
    override val code: String,
    override val type: FrontofficeApplicationErrorType,
    override val message: String,
) : FrontofficeApplicationErrorCode {
    STAFF_NOT_BELONG_TO_PERFORMANCE("STAFF_NOT_BELONG_TO_PERFORMANCE", FrontofficeApplicationErrorType.FORBIDDEN, "해당 스태프는 해당 공연에 속해있지 않습니다."),
    STAFF_NOT_FOUND("STAFF_NOT_FOUND", FrontofficeApplicationErrorType.NOT_FOUND, "스태프가 존재하지 않습니다."),
}
