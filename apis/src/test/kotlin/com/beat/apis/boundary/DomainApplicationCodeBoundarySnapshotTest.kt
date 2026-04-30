package com.beat.apis.boundary

import com.beat.apis.booking.api.response.BookingSuccessCode
import com.beat.apis.ticket.api.response.TicketSuccessCode
import com.beat.apis.booking.application.exception.BookingApplicationErrorCode
import com.beat.apis.ticket.application.exception.TicketApplicationErrorCode
import com.beat.apis.external.s3.exception.FileSuccessCode
import com.beat.apis.member.api.response.MemberSuccessCode
import com.beat.apis.member.application.exception.MemberApplicationErrorCode
import com.beat.apis.performance.api.response.PerformanceSuccessCode
import com.beat.apis.performance.application.exception.CastApplicationErrorCode
import com.beat.apis.performance.application.exception.PerformanceApplicationErrorCode
import com.beat.apis.performance.application.exception.PerformanceImageApplicationErrorCode
import com.beat.apis.performance.application.exception.StaffApplicationErrorCode
import com.beat.apis.schedule.api.response.ScheduleSuccessCode
import com.beat.apis.schedule.application.exception.ScheduleApplicationErrorCode
import com.beat.apis.user.application.exception.UserApplicationErrorCode
import com.beat.global.common.exception.base.BaseErrorCode
import com.beat.global.common.exception.base.BaseSuccessCode
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class DomainApplicationCodeBoundarySnapshotTest {
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
            success(PerformanceSuccessCode.HOME_PERFORMANCE_RETRIEVE_SUCCESS, 200, "홈 화면 공연 목록 조회가 성공적으로 완료되었습니다."),
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
            error(PerformanceApplicationErrorCode.SCHEDULE_LIST_NOT_FOUND, 404, "스케쥴 리스트에 스케쥴이 없습니다."),
            error(PerformanceImageApplicationErrorCode.PERFORMANCE_IMAGE_NOT_FOUND, 404, "해당 공연 상세이미지를 찾을 수 없습니다."),
            error(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND, 404, "해당 회차를 찾을 수 없습니다."),
            error(StaffApplicationErrorCode.STAFF_NOT_FOUND, 404, "스태프가 존재하지 않습니다."),
            error(UserApplicationErrorCode.USER_NOT_FOUND, 404, "유저가 없습니다"),
        )

        assertAll(snapshots.map { snapshot -> Executable { assertErrorSnapshot(snapshot) } })
    }

    private fun success(code: BaseSuccessCode, status: Int, message: String): SuccessSnapshot =
        SuccessSnapshot(code, status, message)

    private fun error(code: BaseErrorCode, status: Int, message: String): ErrorSnapshot =
        ErrorSnapshot(code, status, message)

    private fun assertSuccessSnapshot(snapshot: SuccessSnapshot) {
        assertEquals(snapshot.status, snapshot.code.getStatus(), "${snapshot.code} status")
        assertEquals(snapshot.message, snapshot.code.getMessage(), "${snapshot.code} message")
    }

    private fun assertErrorSnapshot(snapshot: ErrorSnapshot) {
        assertEquals(snapshot.status, snapshot.code.getStatus(), "${snapshot.code} status")
        assertEquals(snapshot.message, snapshot.code.getMessage(), "${snapshot.code} message")
    }

    private data class SuccessSnapshot(
        val code: BaseSuccessCode,
        val status: Int,
        val message: String,
    )

    private data class ErrorSnapshot(
        val code: BaseErrorCode,
        val status: Int,
        val message: String,
    )
}
