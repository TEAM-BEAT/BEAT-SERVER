package com.beat.application.frontoffice.performance.exception

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType

enum class PerformanceImageApplicationErrorCode(
    override val code: String,
    override val type: FrontofficeApplicationErrorType,
    override val message: String,
) : FrontofficeApplicationErrorCode {
    PERFORMANCE_IMAGE_NOT_BELONG_TO_PERFORMANCE(
        "PERFORMANCE_IMAGE_NOT_BELONG_TO_PERFORMANCE",
        FrontofficeApplicationErrorType.FORBIDDEN,
        "해당 상세이미지는 해당 공연에 속해 있지 않습니다.",
    ),
    PERFORMANCE_IMAGE_NOT_FOUND(
        "PERFORMANCE_IMAGE_NOT_FOUND",
        FrontofficeApplicationErrorType.NOT_FOUND,
        "해당 공연 상세이미지를 찾을 수 없습니다.",
    ),
}
