package com.beat.apis.member.application.exception

import com.beat.global.support.exception.base.BaseErrorCode

enum class MemberApplicationErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    SOCIAL_TYPE_BAD_REQUEST(400, "로그인 요청이 유효하지 않습니다."),
    AUTHENTICATION_CODE_EXPIRED(401, "인가코드가 만료되었습니다"),
    MEMBER_NOT_FOUND(404, "회원이 없습니다")
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
