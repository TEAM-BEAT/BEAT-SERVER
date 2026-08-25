package com.beat.application.admin.user.exception

import com.beat.application.admin.exception.AdminApplicationErrorCode
import com.beat.application.admin.exception.AdminApplicationErrorType

enum class UserApplicationErrorCode(
    override val code: String,
    override val type: AdminApplicationErrorType,
    override val message: String,
) : AdminApplicationErrorCode {
    MEMBER_NOT_FOUND("ADMIN_USER_MEMBER_NOT_FOUND", AdminApplicationErrorType.NOT_FOUND, "회원이 없습니다")
}
