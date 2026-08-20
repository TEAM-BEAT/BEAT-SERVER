package com.beat.apis.boundary

import com.beat.apis.booking.api.response.BookingSuccessCode
import com.beat.apis.ticket.api.response.TicketSuccessCode
import com.beat.apis.booking.exception.BookingApplicationErrorCode
import com.beat.apis.exception.ApiGlobalExceptionHandler
import com.beat.apis.ticket.exception.TicketApplicationErrorCode
import com.beat.apis.file.api.response.FileSuccessCode
import com.beat.apis.home.api.response.HomeSuccessCode
import com.beat.apis.member.api.response.MemberSuccessCode
import com.beat.apis.member.exception.MemberApplicationErrorCode
import com.beat.apis.performance.api.response.PerformanceSuccessCode
import com.beat.apis.schedule.api.response.ScheduleSuccessCode
import com.beat.apis.user.exception.UserApplicationErrorCode
import com.beat.apis.exception.ApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorCode
import com.beat.application.frontoffice.performance.exception.CastApplicationErrorCode
import com.beat.application.frontoffice.performance.exception.PerformanceApplicationErrorCode
import com.beat.application.frontoffice.performance.exception.PerformanceImageApplicationErrorCode
import com.beat.application.frontoffice.performance.exception.StaffApplicationErrorCode
import com.beat.application.frontoffice.schedule.exception.ScheduleApplicationErrorCode
import com.beat.domain.booking.exception.BookingErrorCode
import com.beat.domain.exception.DomainErrorType
import com.beat.domain.performance.exception.PerformanceErrorCode
import com.beat.domain.promotion.exception.PromotionErrorCode
import com.beat.domain.schedule.exception.ScheduleErrorCode
import com.beat.apis.member.exception.TokenApplicationErrorCode
import com.beat.global.support.response.SuccessCode
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class DomainApplicationCodeBoundarySnapshotTest {
    @Test
    fun errorCodesAreExplicitAndUniqueWithinTheirLayer() {
        val applicationCodes =
            BookingApplicationErrorCode.entries.map { it.getCode() } +
                TicketApplicationErrorCode.entries.map { it.getCode() } +
                MemberApplicationErrorCode.entries.map { it.getCode() } +
                CastApplicationErrorCode.entries.map { it.code } +
                PerformanceApplicationErrorCode.entries.map { it.code } +
                PerformanceImageApplicationErrorCode.entries.map { it.code } +
                StaffApplicationErrorCode.entries.map { it.code } +
                ScheduleApplicationErrorCode.entries.map { it.code } +
                UserApplicationErrorCode.entries.map { it.getCode() } +
                TokenApplicationErrorCode.entries.map { it.getCode() }
        val domainCodes =
            BookingErrorCode.entries.map { it.code } +
                PerformanceErrorCode.entries.map { it.code } +
                PromotionErrorCode.entries.map { it.code } +
                ScheduleErrorCode.entries.map { it.code }

        assertEquals(applicationCodes.size, applicationCodes.distinct().size)
        assertEquals(domainCodes.size, domainCodes.distinct().size)
        assertTrue(applicationCodes.all { it.matches(Regex("[A-Z][A-Z0-9_]+")) })
        assertTrue(domainCodes.all { it.matches(Regex("[A-Z][A-Z0-9_]+")) })
    }

    @Test
    fun successCodeStatusAndMessagesStayStableAcrossResponseBoundaryMove() {
        val snapshots = listOf(
            success(BookingSuccessCode.MEMBER_BOOKING_RETRIEVE_SUCCESS, 200, "회원 예매 조회가 성공적으로 완료되었습니다."),
            success(BookingSuccessCode.GUEST_BOOKING_RETRIEVE_SUCCESS, 200, "비회원 예매 조회가 성공적으로 완료되었습니다."),
            success(BookingSuccessCode.BOOKING_REFUND_SUCCESS, 200, "예매자의 환불요청이 성공했습니다."),
            success(BookingSuccessCode.BOOKING_CANCEL_SUCCESS, 200, "예매자의 취소요청이 성공했습니다."),
            success(BookingSuccessCode.MEMBER_BOOKING_SUCCESS, 201, "회원 예매가 성공적으로 완료되었습니다"),
            success(BookingSuccessCode.GUEST_BOOKING_SUCCESS, 201, "비회원 예매가 성공적으로 완료되었습니다"),
            success(TicketSuccessCode.TICKET_RETRIEVE_SUCCESS, 200, "예매자 목록 조회가 성공적으로 완료되었습니다."),
            success(TicketSuccessCode.TICKET_UPDATE_SUCCESS, 200, "예매자 입금여부 수정이 성공적으로 완료되었습니다."),
            success(TicketSuccessCode.TICKET_REFUND_SUCCESS, 200, "예매 환불처리 요청이 성공했습니다."),
            success(TicketSuccessCode.TICKET_DELETE_SUCCESS, 200, "예매자 삭제 요청이 성공했습니다."),
            success(TicketSuccessCode.TICKET_SEARCH_SUCCESS, 200, "예매자 검색 결과 조회가 성공적으로 완료되었습니다."),
            success(MemberSuccessCode.SIGN_UP_SUCCESS, 200, "로그인 성공"),
            success(MemberSuccessCode.ISSUE_ACCESS_TOKEN_SUCCESS, 200, "엑세스토큰 발급 성공"),
            success(MemberSuccessCode.ISSUE_ACCESS_TOKEN_USING_REFRESH_TOKEN, 200, "리프레쉬 토큰으로 액세스 토큰 재발급 성공"),
            success(MemberSuccessCode.SIGN_OUT_SUCCESS, 200, "로그아웃 성공"),
            success(MemberSuccessCode.USER_DELETE_SUCCESS, 200, "회원 탈퇴 성공"),
            success(PerformanceSuccessCode.PERFORMANCE_UPDATE_SUCCESS, 200, "공연이 성공적으로 수정되었습니다."),
            success(PerformanceSuccessCode.PERFORMANCE_RETRIEVE_SUCCESS, 200, "공연 상세 정보 조회가 성공적으로 완료되었습니다."),
            success(PerformanceSuccessCode.PERFORMANCE_MODIFY_PAGE_SUCCESS, 200, "공연 수정 페이지 조회가 성공적으로 완료되었습니다."),
            success(PerformanceSuccessCode.PERFORMANCE_DELETE_SUCCESS, 200, "공연이 성공적으로 삭제되었습니다."),
            success(PerformanceSuccessCode.BOOKING_PERFORMANCE_RETRIEVE_SUCCESS, 200, "예매 관련 공연 정보 조회가 성공적으로 완료되었습니다."),
            success(HomeSuccessCode.HOME_PERFORMANCE_RETRIEVE_SUCCESS, 200, "홈 화면 공연 목록 조회가 성공적으로 완료되었습니다."),
            success(PerformanceSuccessCode.MAKER_PERFORMANCE_RETRIEVE_SUCCESS, 200, "회원이 등록한 공연 목록의 조회가 성공적으로 완료되었습니다."),
            success(PerformanceSuccessCode.PERFORMANCE_CREATE_SUCCESS, 201, "공연이 성공적으로 생성되었습니다."),
            success(ScheduleSuccessCode.TICKET_AVAILABILITY_RETRIEVAL_SUCCESS, 200, "티켓 수량 조회가 성공적으로 완료되었습니다."),
            success(FileSuccessCode.PERFORMANCE_MAKER_PRESIGNED_URL_ISSUED, 200, "공연 메이커를 위한 Presigned URL 발급 성공"),
        )

        assertAll(snapshots.map { snapshot -> Executable { assertSuccessSnapshot(snapshot) } })
    }

    @Test
    fun lookupNotFoundErrorCodeStatusAndMessagesStayStableAcrossApplicationBoundaryMove() {
        val snapshots = listOf(
            error(BookingApplicationErrorCode.NO_BOOKING_FOUND, 404, "입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요."),
            error(TicketApplicationErrorCode.NO_TICKETS_FOUND, 404, "입력하신 정보와 일치하는 예매자 목록이 없습니다."),
            error(BookingApplicationErrorCode.NO_PERFORMANCE_FOUND, 404, "공연을 찾을 수 없습니다."),
            error(BookingApplicationErrorCode.NO_SCHEDULE_FOUND, 404, "회차를 찾을 수 없습니다."),
            error(CastApplicationErrorCode.CAST_NOT_FOUND, 404, "등장인물이 존재하지 않습니다."),
            error(MemberApplicationErrorCode.MEMBER_NOT_FOUND, 404, "회원이 없습니다"),
            error(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND, 404, "해당 공연 정보를 찾을 수 없습니다."),
            error(PerformanceApplicationErrorCode.SCHEDULE_LIST_NOT_FOUND, 400, "스케쥴 리스트에 스케쥴이 없습니다."),
            error(PerformanceImageApplicationErrorCode.PERFORMANCE_IMAGE_NOT_FOUND, 404, "해당 공연 상세이미지를 찾을 수 없습니다."),
            error(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND, 404, "해당 회차를 찾을 수 없습니다."),
            error(StaffApplicationErrorCode.STAFF_NOT_FOUND, 404, "스태프가 존재하지 않습니다."),
            error(UserApplicationErrorCode.USER_NOT_FOUND, 404, "유저가 없습니다"),
        )

        assertAll(snapshots.map { snapshot -> Executable { assertErrorSnapshot(snapshot) } })
    }

    @Test
    fun requestActorExternalErrorCodeStatusAndMessagesStayStableAcrossApplicationBoundaryMove() {
        val snapshots = listOf(
            error(BookingApplicationErrorCode.REQUIRED_DATA_MISSING, 400, "필수 데이터가 누락되었습니다."),
            error(BookingApplicationErrorCode.INVALID_REQUEST_FORMAT, 400, "잘못된 요청 형식입니다."),
            error(
                BookingApplicationErrorCode.INSUFFICIENT_TICKETS,
                400,
                "요청한 티켓 수량이 잔여 티켓 수를 초과했습니다. 다른 수량을 선택해 주세요.",
            ),
            error(MemberApplicationErrorCode.SOCIAL_TYPE_BAD_REQUEST, 400, "로그인 요청이 유효하지 않습니다."),
            error(MemberApplicationErrorCode.AUTHENTICATION_CODE_EXPIRED, 401, "인가코드가 만료되었습니다"),
            error(PerformanceApplicationErrorCode.PRICE_UPDATE_NOT_ALLOWED, 400, "예매자가 존재하여 가격을 수정할 수 없습니다."),
            error(PerformanceApplicationErrorCode.PAST_SCHEDULE_NOT_ALLOWED, 400, "과거 날짜 회차를 포함한 공연을 생성할 수 없습니다."),
            error(PerformanceApplicationErrorCode.SCHEDULE_MODIFICATION_NOT_ALLOWED_FOR_ENDED_SCHEDULE, 400, "종료된 회차를 수정할 수 없습니다."),
            error(PerformanceApplicationErrorCode.PERFORMANCE_DELETE_FAILED, 403, "예매자가 1명 이상 있을 경우, 공연을 삭제할 수 없습니다."),
            error(PerformanceApplicationErrorCode.NOT_PERFORMANCE_OWNER, 403, "해당 공연의 메이커가 아닙니다."),
            error(CastApplicationErrorCode.CAST_NOT_BELONG_TO_PERFORMANCE, 403, "해당 등장인물은 해당 공연에 속해 있지 않습니다."),
            error(PerformanceImageApplicationErrorCode.PERFORMANCE_IMAGE_NOT_BELONG_TO_PERFORMANCE, 403, "해당 싱세이미지는 해당 공연에 속해 있지 않습니다."),
            error(ScheduleApplicationErrorCode.INVALID_TICKET_AVAILABILITY_REQUEST, 400, "잘못된 데이터 형식입니다."),
            error(ScheduleApplicationErrorCode.SCHEDULE_NOT_BELONG_TO_PERFORMANCE, 403, "해당 스케줄은 해당 공연에 속해 있지 않습니다."),
            error(ScheduleApplicationErrorCode.INSUFFICIENT_TICKETS, 409, "요청한 티켓 수량이 잔여 티켓 수를 초과했습니다. 다른 수량을 선택해 주세요."),
            error(StaffApplicationErrorCode.STAFF_NOT_BELONG_TO_PERFORMANCE, 403, "해당 스태프는 해당 공연에 속해있지 않습니다."),
            error(TicketApplicationErrorCode.PAYMENT_COMPLETED_TICKET_UPDATE_NOT_ALLOWED, 400, "이미 결제가 완료된 티켓의 상태는 변경할 수 없습니다."),
            error(TicketApplicationErrorCode.INVALID_BOOKING_STATUS_TRANSITION, 400, "지원하지 않는 예매 상태 변경입니다."),
            error(TicketApplicationErrorCode.DELETED_TICKET_RETRIEVE_NOT_ALLOWED, 400, "삭제된 예매자를 조회할 수 없습니다."),
        )

        assertAll(snapshots.map { snapshot -> Executable { assertErrorSnapshot(snapshot) } })
    }

    @Test
    fun domainErrorCodesOnlyExposeDomainInvariantAllowlist() {
        assertEquals(
            listOf(
                "INVALID_PURCHASE_TICKET_COUNT",
                "PURCHASE_TICKET_COUNT_EXCEEDED",
                "NEGATIVE_TOTAL_PAYMENT_AMOUNT",
                "INVALID_REFUND_ACCOUNT",
                "PAYMENT_CONFIRMATION_NOT_ALLOWED",
                "CONFIRMED_STATUS_CHANGE_NOT_ALLOWED",
                "STATUS_TRANSITION_NOT_ALLOWED",
                "REFUND_REQUEST_NOT_ALLOWED",
                "CANCELLATION_NOT_ALLOWED",
                "REFUND_COMPLETION_NOT_ALLOWED",
                "DELETION_NOT_ALLOWED",
            ),
            BookingErrorCode.entries.map { it.name },
        )
        assertEquals(DomainErrorType.INVALID_INPUT, BookingErrorCode.INVALID_PURCHASE_TICKET_COUNT.type)
        assertEquals(DomainErrorType.INVALID_INPUT, BookingErrorCode.PURCHASE_TICKET_COUNT_EXCEEDED.type)
        assertEquals(DomainErrorType.STATE_CONFLICT, BookingErrorCode.PAYMENT_CONFIRMATION_NOT_ALLOWED.type)
        assertEquals(
            listOf(
                "NON_POSITIVE_RUNNING_TIME",
                "NEGATIVE_SCHEDULE_COUNT",
                "INVALID_PERFORMANCE_PERIOD",
                "NEGATIVE_TICKET_PRICE",
                "PRICE_UPDATE_NOT_ALLOWED",
                "DELETE_NOT_ALLOWED",
                "NEGATIVE_TICKET_QUANTITY",
                "INCOMPLETE_PAYMENT_ACCOUNT",
            ),
            PerformanceErrorCode.entries.map { it.name },
        )
        assertEquals(listOf("TOO_MANY_CAROUSEL_PROMOTIONS"), PromotionErrorCode.entries.map { it.name })
        assertEquals(
            listOf(
                "INVALID_BOOKING_WINDOW",
                "NEGATIVE_TICKET_COUNT",
                "NON_POSITIVE_TICKET_COUNT",
                "MIXED_PERFORMANCE_SCHEDULES",
                "TOO_MANY_SCHEDULES",
                "PAST_SCHEDULE_NOT_ALLOWED",
                "ENDED_SCHEDULE_MODIFICATION_NOT_ALLOWED",
                "ALLOCATED_TICKETS_EXCEED_TOTAL",
                "INSUFFICIENT_TICKETS",
                "EXCESS_TICKET_DELETE",
            ),
            ScheduleErrorCode.entries.map { it.name },
        )
        assertEquals(DomainErrorType.INVALID_INPUT, ScheduleErrorCode.NON_POSITIVE_TICKET_COUNT.type)
        assertEquals(DomainErrorType.STATE_CONFLICT, ScheduleErrorCode.INSUFFICIENT_TICKETS.type)
    }

    private fun success(code: SuccessCode, status: Int, message: String): SuccessSnapshot =
        SuccessSnapshot(code, status, message)

    private fun error(code: ApplicationErrorCode, status: Int, message: String): ErrorSnapshot {
        val snapshot = ErrorSnapshot(code.getCode(), statusOf(code), code.getMessage())
        assertEquals(status, snapshot.status, "${code} status")
        assertEquals(message, snapshot.message, "${code} message")
        return snapshot
    }

    private fun error(code: FrontofficeApplicationErrorCode, status: Int, message: String): ErrorSnapshot {
        val snapshot = ErrorSnapshot(code.code, statusOf(code), code.message)
        assertEquals(status, snapshot.status, "${code} status")
        assertEquals(message, snapshot.message, "${code} message")
        return snapshot
    }

    private fun assertSuccessSnapshot(snapshot: SuccessSnapshot) {
        assertEquals(snapshot.status, snapshot.code.getStatus(), "${snapshot.code} status")
        assertEquals(snapshot.message, snapshot.code.getMessage(), "${snapshot.code} message")
    }

    private fun assertErrorSnapshot(snapshot: ErrorSnapshot) {
        assertTrue(snapshot.code.matches(Regex("[A-Z][A-Z0-9_]+")))
    }

    private fun statusOf(code: ApplicationErrorCode): Int = ApiGlobalExceptionHandler.toHttpStatus(code).value()

    private fun statusOf(code: FrontofficeApplicationErrorCode): Int =
        ApiGlobalExceptionHandler.toHttpStatus(code.type).value()

    private data class SuccessSnapshot(
        val code: SuccessCode,
        val status: Int,
        val message: String,
    )

    private data class ErrorSnapshot(
        val code: String,
        val status: Int,
        val message: String,
    )
}
