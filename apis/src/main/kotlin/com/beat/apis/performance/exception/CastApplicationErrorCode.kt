package com.beat.apis.performance.exception

import com.beat.apis.exception.ApplicationErrorCode
import com.beat.apis.exception.ApplicationErrorType

enum class CastApplicationErrorCode(
    private val code: String,
    private val type: ApplicationErrorType,
    private val message: String,
) : ApplicationErrorCode {
    CAST_NOT_BELONG_TO_PERFORMANCE(
        "CAST_NOT_BELONG_TO_PERFORMANCE",
        ApplicationErrorType.FORBIDDEN,
        "해당 등장인물은 해당 공연에 속해 있지 않습니다.",
    ),
    CAST_NOT_FOUND("CAST_NOT_FOUND", ApplicationErrorType.NOT_FOUND, "등장인물이 존재하지 않습니다.")
    ;

    override fun getCode(): String = code

    override fun getType(): ApplicationErrorType = type

    override fun getMessage(): String = message
}
