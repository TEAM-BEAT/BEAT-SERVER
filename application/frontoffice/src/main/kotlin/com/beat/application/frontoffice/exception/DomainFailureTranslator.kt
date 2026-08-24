package com.beat.application.frontoffice.exception

import com.beat.domain.booking.exception.BookingErrorCode
import com.beat.domain.exception.DomainErrorCode
import com.beat.domain.exception.DomainErrorType
import com.beat.domain.exception.DomainException
import com.beat.domain.performance.exception.PerformanceErrorCode
import com.beat.domain.schedule.exception.ScheduleErrorCode

internal fun <T> translateDomainFailure(block: () -> T): T =
    try {
        block()
    } catch (exception: DomainException) {
        throw FrontofficeApplicationException(
            DomainApplicationErrorCode.from(exception.errorCode),
            exception,
        )
    }

internal data class DomainApplicationErrorCode(
    override val code: String,
    override val type: FrontofficeApplicationErrorType,
    override val message: String,
) : FrontofficeApplicationErrorCode {
    companion object {
        fun from(errorCode: DomainErrorCode): DomainApplicationErrorCode =
            DomainApplicationErrorCode(
                code = errorCode.code,
                type = errorCode.applicationType(),
                message = errorCode.applicationMessage(),
            )
    }
}

private fun DomainErrorCode.applicationMessage(): String = when (this) {
    BookingErrorCode.INVALID_PURCHASE_TICKET_COUNT,
    BookingErrorCode.INVALID_REFUND_ACCOUNT,
    BookingErrorCode.PAYMENT_CONFIRMATION_NOT_ALLOWED,
    BookingErrorCode.REFUND_REQUEST_NOT_ALLOWED,
    PerformanceErrorCode.NON_POSITIVE_RUNNING_TIME,
    PerformanceErrorCode.NEGATIVE_SCHEDULE_COUNT,
    ScheduleErrorCode.INVALID_BOOKING_WINDOW,
    ScheduleErrorCode.NEGATIVE_TICKET_COUNT,
    ScheduleErrorCode.NON_POSITIVE_TICKET_COUNT -> "잘못된 데이터 형식입니다."

    ScheduleErrorCode.TOO_MANY_SCHEDULES -> "공연 회차는 최대 10개까지 추가할 수 있습니다."
    ScheduleErrorCode.ALLOCATED_TICKETS_EXCEED_TOTAL ->
        "판매된 티켓 수보다 적은 수로 판매할 티켓 매수를 수정할 수 없습니다."

    else -> message
}

private fun DomainErrorCode.applicationType(): FrontofficeApplicationErrorType = when (this) {
    BookingErrorCode.PAYMENT_CONFIRMATION_NOT_ALLOWED,
    BookingErrorCode.REFUND_REQUEST_NOT_ALLOWED,
    BookingErrorCode.CONFIRMED_STATUS_CHANGE_NOT_ALLOWED,
    BookingErrorCode.STATUS_TRANSITION_NOT_ALLOWED,
    BookingErrorCode.CANCELLATION_NOT_ALLOWED,
    BookingErrorCode.REFUND_COMPLETION_NOT_ALLOWED,
    BookingErrorCode.DELETION_NOT_ALLOWED,
    ScheduleErrorCode.INSUFFICIENT_TICKETS,
    ScheduleErrorCode.ENDED_SCHEDULE_MODIFICATION_NOT_ALLOWED,
    PerformanceErrorCode.PRICE_UPDATE_NOT_ALLOWED -> FrontofficeApplicationErrorType.INVALID_INPUT

    PerformanceErrorCode.DELETE_NOT_ALLOWED -> FrontofficeApplicationErrorType.FORBIDDEN
    else -> when (type) {
        DomainErrorType.INVALID_INPUT -> FrontofficeApplicationErrorType.INVALID_INPUT
        DomainErrorType.STATE_CONFLICT -> FrontofficeApplicationErrorType.STATE_CONFLICT
    }
}
