package com.beat.apis.performance.application.exception

import com.beat.global.common.exception.base.BaseErrorCode

enum class CastApplicationErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    CAST_NOT_BELONG_TO_PERFORMANCE(403, "해당 등장인물은 해당 공연에 속해 있지 않습니다."),
    CAST_NOT_FOUND(404, "등장인물이 존재하지 않습니다.")
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
