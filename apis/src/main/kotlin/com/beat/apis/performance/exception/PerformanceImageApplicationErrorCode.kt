package com.beat.apis.performance.exception

import com.beat.apis.exception.ApplicationErrorCode
import com.beat.apis.exception.ApplicationErrorType

enum class PerformanceImageApplicationErrorCode(
    private val code: String,
    private val type: ApplicationErrorType,
    private val message: String,
) : ApplicationErrorCode {
    PERFORMANCE_IMAGE_NOT_BELONG_TO_PERFORMANCE(
        "PERFORMANCE_IMAGE_NOT_BELONG_TO_PERFORMANCE",
        ApplicationErrorType.FORBIDDEN,
        "해당 싱세이미지는 해당 공연에 속해 있지 않습니다.",
    ),
    PERFORMANCE_IMAGE_NOT_FOUND(
        "PERFORMANCE_IMAGE_NOT_FOUND",
        ApplicationErrorType.NOT_FOUND,
        "해당 공연 상세이미지를 찾을 수 없습니다.",
    )
    ;

    override fun getCode(): String = code

    override fun getType(): ApplicationErrorType = type

    override fun getMessage(): String = message
}
