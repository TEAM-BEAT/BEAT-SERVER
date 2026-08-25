package com.beat.domain.schedule.exception

import com.beat.domain.exception.DomainErrorCode
import com.beat.domain.exception.DomainErrorType

enum class ScheduleErrorCode(
    override val code: String,
    override val type: DomainErrorType,
    override val message: String,
) : DomainErrorCode {
    INVALID_BOOKING_WINDOW(
        "SCHEDULE_INVALID_BOOKING_WINDOW",
        DomainErrorType.INVALID_INPUT,
        "예매 마감 시각은 공연 시작 시각보다 빠를 수 없습니다.",
    ),
    NEGATIVE_TICKET_COUNT(
        "SCHEDULE_NEGATIVE_TICKET_COUNT",
        DomainErrorType.INVALID_INPUT,
        "티켓 수량은 음수일 수 없습니다.",
    ),
    NON_POSITIVE_TICKET_COUNT(
        "SCHEDULE_NON_POSITIVE_TICKET_COUNT",
        DomainErrorType.INVALID_INPUT,
        "처리할 티켓 수량은 1개 이상이어야 합니다.",
    ),
    MIXED_PERFORMANCE_SCHEDULES(
        "SCHEDULE_MIXED_PERFORMANCE_SCHEDULES",
        DomainErrorType.INVALID_INPUT,
        "서로 다른 공연의 회차를 함께 정렬할 수 없습니다.",
    ),
    TOO_MANY_SCHEDULES(
        "SCHEDULE_TOO_MANY_SCHEDULES",
        DomainErrorType.INVALID_INPUT,
        "지원 가능한 회차 수를 초과했습니다.",
    ),
    PAST_SCHEDULE_NOT_ALLOWED(
        "SCHEDULE_PAST_SCHEDULE_NOT_ALLOWED",
        DomainErrorType.INVALID_INPUT,
        "과거 날짜 회차를 포함한 공연을 생성할 수 없습니다.",
    ),
    ENDED_SCHEDULE_MODIFICATION_NOT_ALLOWED(
        "SCHEDULE_ENDED_MODIFICATION_NOT_ALLOWED",
        DomainErrorType.STATE_CONFLICT,
        "종료된 회차를 수정할 수 없습니다.",
    ),
    ALLOCATED_TICKETS_EXCEED_TOTAL(
        "SCHEDULE_ALLOCATED_TICKETS_EXCEED_TOTAL",
        DomainErrorType.INVALID_INPUT,
        "예매된 티켓 수는 전체 티켓 수를 초과할 수 없습니다.",
    ),
    INSUFFICIENT_TICKETS(
        "SCHEDULE_INSUFFICIENT_TICKETS",
        DomainErrorType.STATE_CONFLICT,
        "요청한 티켓 수량이 잔여 티켓 수를 초과했습니다. 다른 수량을 선택해 주세요.",
    ),
    EXCESS_TICKET_DELETE(
        "SCHEDULE_EXCESS_TICKET_DELETE",
        DomainErrorType.STATE_CONFLICT,
        "예매된 티켓 수 이상을 삭제할 수 없습니다.",
    ),
}
