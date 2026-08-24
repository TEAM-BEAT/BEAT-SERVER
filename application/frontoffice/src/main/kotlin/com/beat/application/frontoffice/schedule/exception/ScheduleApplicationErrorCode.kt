package com.beat.application.frontoffice.schedule.exception

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType

enum class ScheduleApplicationErrorCode(
    override val code: String,
    override val type: FrontofficeApplicationErrorType,
    override val message: String,
) : FrontofficeApplicationErrorCode {
    INVALID_TICKET_AVAILABILITY_REQUEST("SCHEDULE_INVALID_TICKET_AVAILABILITY_REQUEST", FrontofficeApplicationErrorType.INVALID_INPUT, "잘못된 데이터 형식입니다."),
    SCHEDULE_NOT_BELONG_TO_PERFORMANCE("SCHEDULE_NOT_BELONG_TO_PERFORMANCE", FrontofficeApplicationErrorType.FORBIDDEN, "해당 스케줄은 해당 공연에 속해 있지 않습니다."),
    NO_SCHEDULE_FOUND("SCHEDULE_NOT_FOUND", FrontofficeApplicationErrorType.NOT_FOUND, "해당 회차를 찾을 수 없습니다."),
    INSUFFICIENT_TICKETS("SCHEDULE_INSUFFICIENT_TICKETS", FrontofficeApplicationErrorType.STATE_CONFLICT, "요청한 티켓 수량이 잔여 티켓 수를 초과했습니다. 다른 수량을 선택해 주세요."),
    BOOKING_CLOSED("SCHEDULE_BOOKING_CLOSED", FrontofficeApplicationErrorType.STATE_CONFLICT, "예매가 마감된 회차입니다."),
}
