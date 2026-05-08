package com.beat.apis.performance.api.response

import com.beat.global.support.exception.base.BaseSuccessCode

enum class PerformanceSuccessCode(
    private val status: Int,
    private val message: String,
) : BaseSuccessCode {
    PERFORMANCE_UPDATE_SUCCESS(200, "공연이 성공적으로 수정되었습니다."),
    PERFORMANCE_RETRIEVE_SUCCESS(200, "공연 상세 정보 조회가 성공적으로 완료되었습니다."),
    PERFORMANCE_MODIFY_PAGE_SUCCESS(200, "공연 수정 페이지 조회가 성공적으로 완료되었습니다."),
    PERFORMANCE_DELETE_SUCCESS(200, "공연이 성공적으로 삭제되었습니다."),
    BOOKING_PERFORMANCE_RETRIEVE_SUCCESS(200, "예매 관련 공연 정보 조회가 성공적으로 완료되었습니다."),
    MAKER_PERFORMANCE_RETRIEVE_SUCCESS(200, "회원이 등록한 공연 목록의 조회가 성공적으로 완료되었습니다."),
    PERFORMANCE_CREATE_SUCCESS(201, "공연이 성공적으로 생성되었습니다.")
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
