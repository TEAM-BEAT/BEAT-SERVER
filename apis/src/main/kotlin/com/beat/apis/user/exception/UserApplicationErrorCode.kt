package com.beat.apis.user.exception

import com.beat.apis.exception.ApplicationErrorCode
import com.beat.apis.exception.ApplicationErrorType

enum class UserApplicationErrorCode(
    private val code: String,
    private val type: ApplicationErrorType,
    private val message: String,
) : ApplicationErrorCode {
    USER_NOT_FOUND("USER_NOT_FOUND", ApplicationErrorType.NOT_FOUND, "유저가 없습니다")
    ;

    override fun getCode(): String = code

    override fun getType(): ApplicationErrorType = type

    override fun getMessage(): String = message
}
