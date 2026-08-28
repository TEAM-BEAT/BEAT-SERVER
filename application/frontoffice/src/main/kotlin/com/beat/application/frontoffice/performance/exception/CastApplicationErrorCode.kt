package com.beat.application.frontoffice.performance.exception

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType

enum class CastApplicationErrorCode(
    override val code: String,
    override val type: FrontofficeApplicationErrorType,
    override val message: String,
) : FrontofficeApplicationErrorCode {
    CAST_NOT_BELONG_TO_PERFORMANCE(
        "CAST_NOT_BELONG_TO_PERFORMANCE",
        FrontofficeApplicationErrorType.FORBIDDEN,
        "해당 등장인물은 해당 공연에 속해 있지 않습니다.",
    ),
    CAST_NOT_FOUND("CAST_NOT_FOUND", FrontofficeApplicationErrorType.NOT_FOUND, "등장인물이 존재하지 않습니다."),
}
