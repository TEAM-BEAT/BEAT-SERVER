package com.beat.apis.performance.exception

import com.beat.apis.exception.ApplicationErrorCode
import com.beat.apis.exception.ApplicationErrorType

enum class StaffApplicationErrorCode(
    private val code: String,
    private val type: ApplicationErrorType,
    private val message: String,
) : ApplicationErrorCode {
    STAFF_NOT_BELONG_TO_PERFORMANCE(
        "STAFF_NOT_BELONG_TO_PERFORMANCE",
        ApplicationErrorType.FORBIDDEN,
        "해당 스태프는 해당 공연에 속해있지 않습니다.",
    ),
    STAFF_NOT_FOUND("STAFF_NOT_FOUND", ApplicationErrorType.NOT_FOUND, "스태프가 존재하지 않습니다.")
    ;

    override fun getCode(): String = code

    override fun getType(): ApplicationErrorType = type

    override fun getMessage(): String = message
}
