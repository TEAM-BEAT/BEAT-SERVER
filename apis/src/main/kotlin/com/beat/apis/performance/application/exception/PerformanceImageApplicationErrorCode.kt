package com.beat.apis.performance.application.exception

import com.beat.global.support.exception.base.BaseErrorCode

enum class PerformanceImageApplicationErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    PERFORMANCE_IMAGE_NOT_BELONG_TO_PERFORMANCE(403, "해당 싱세이미지는 해당 공연에 속해 있지 않습니다."),
    PERFORMANCE_IMAGE_NOT_FOUND(404, "해당 공연 상세이미지를 찾을 수 없습니다.")
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
