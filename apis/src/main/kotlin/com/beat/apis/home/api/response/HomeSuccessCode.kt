package com.beat.apis.home.api.response

import com.beat.global.common.exception.base.BaseSuccessCode

enum class HomeSuccessCode(
    private val status: Int,
    private val message: String,
) : BaseSuccessCode {
    HOME_PERFORMANCE_RETRIEVE_SUCCESS(200, "홈 화면 공연 목록 조회가 성공적으로 완료되었습니다."),
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
