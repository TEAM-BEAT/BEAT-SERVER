package com.beat.admin.user.exception

import com.beat.admin.exception.ApplicationErrorCode
import com.beat.admin.exception.ApplicationErrorType

enum class UserApplicationErrorCode(
    private val code: String,
    private val type: ApplicationErrorType,
    private val message: String,
) : ApplicationErrorCode {
    MEMBER_NOT_FOUND("ADMIN_USER_MEMBER_NOT_FOUND", ApplicationErrorType.NOT_FOUND, "회원이 없습니다"),
    ;

    override fun getCode(): String = code

    override fun getType(): ApplicationErrorType = type

    override fun getMessage(): String = message
}
