package com.beat.apis.member.application.exception

import com.beat.global.common.exception.base.BaseErrorCode

enum class MemberApplicationErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    MEMBER_NOT_FOUND(404, "회원이 없습니다")
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
