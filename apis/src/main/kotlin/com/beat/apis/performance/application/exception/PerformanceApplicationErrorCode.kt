package com.beat.apis.performance.application.exception

import com.beat.global.support.exception.base.BaseErrorCode

enum class PerformanceApplicationErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    PRICE_UPDATE_NOT_ALLOWED(400, "예매자가 존재하여 가격을 수정할 수 없습니다."),
    MAX_SCHEDULE_LIMIT_EXCEEDED(400, "공연 회차는 최대 10개까지 추가할 수 있습니다."),
    PAST_SCHEDULE_NOT_ALLOWED(400, "과거 날짜 회차를 포함한 공연을 생성할 수 없습니다."),
    SCHEDULE_MODIFICATION_NOT_ALLOWED_FOR_ENDED_SCHEDULE(400, "종료된 회차를 수정할 수 없습니다."),
    INVALID_TICKET_COUNT(400, "판매된 티켓 수보다 적은 수로 판매할 티켓 매수를 수정할 수 없습니다."),
    PERFORMANCE_DELETE_FAILED(403, "예매자가 1명 이상 있을 경우, 공연을 삭제할 수 없습니다."),
    NOT_PERFORMANCE_OWNER(403, "해당 공연의 메이커가 아닙니다."),
    PERFORMANCE_NOT_FOUND(404, "해당 공연 정보를 찾을 수 없습니다."),
    SCHEDULE_LIST_NOT_FOUND(404, "스케쥴 리스트에 스케쥴이 없습니다."),
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
