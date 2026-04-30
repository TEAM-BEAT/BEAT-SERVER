package com.beat.apis.performance.application.exception

import com.beat.global.common.exception.base.BaseErrorCode

enum class PerformanceImageApplicationErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    PERFORMANCE_IMAGE_NOT_FOUND(404, "해당 공연 상세이미지를 찾을 수 없습니다.")
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
