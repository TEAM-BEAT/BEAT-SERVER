package com.beat.apps.admin.user.api.response

import com.beat.apps.admin.response.SuccessCode

enum class UserSuccessCode(
    override val status: Int,
    override val message: String,
) : SuccessCode {
    FETCH_ALL_USERS_SUCCESS(200, "관리자 권한으로 모든 유저 조회에 성공하였습니다."),
    ;

}
