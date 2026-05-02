package com.beat.admin.application.exception

import com.beat.global.common.exception.base.BaseErrorCode

enum class AdminApplicationErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    INVALID_REQUEST_FORMAT(400, "잘못된 요청 형식입니다."),
    MEMBER_NOT_FOUND(404, "회원이 없습니다"),
    PERFORMANCE_NOT_FOUND(404, "해당 공연 정보를 찾을 수 없습니다."),
    PROMOTION_NOT_FOUND(404, "해당 홍보 정보를 찾을 수 없습니다.")
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
