package com.beat.apps.api.home.api.response

import com.beat.apps.api.response.SuccessCode

enum class HomeSuccessCode(
    override val status: Int,
    override val message: String,
) : SuccessCode {
    HOME_PERFORMANCE_RETRIEVE_SUCCESS(200, "홈 화면 공연 목록 조회가 성공적으로 완료되었습니다.")
}
