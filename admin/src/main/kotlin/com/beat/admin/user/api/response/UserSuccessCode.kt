package com.beat.admin.user.api.response

import com.beat.global.support.response.SuccessCode

enum class UserSuccessCode(
    private val status: Int,
    private val message: String,
) : SuccessCode {
    FETCH_ALL_USERS_SUCCESS(200, "관리자 권한으로 모든 유저 조회에 성공하였습니다."),
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
