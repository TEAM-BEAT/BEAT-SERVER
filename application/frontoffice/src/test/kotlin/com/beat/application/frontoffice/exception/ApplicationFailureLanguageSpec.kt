package com.beat.application.frontoffice.exception

import com.beat.application.frontoffice.auth.exception.TokenApplicationErrorCode
import com.beat.application.frontoffice.booking.booker.query.BookerBookingPerformanceReadModel
import com.beat.application.frontoffice.booking.booker.query.BookerBookingReadModel
import com.beat.application.frontoffice.booking.booker.query.BookerBookingReader
import com.beat.application.frontoffice.booking.booker.query.BookerBookingScheduleReadModel
import com.beat.application.frontoffice.booking.booker.query.GuestBookingQueryService
import com.beat.application.frontoffice.member.exception.MemberApplicationErrorCode
import com.beat.domain.booking.exception.BookingErrorCode
import com.beat.domain.exception.DomainErrorCode
import com.beat.domain.exception.DomainErrorType
import com.beat.domain.exception.DomainException
import com.beat.domain.performance.exception.PerformanceErrorCode
import com.beat.domain.schedule.exception.ScheduleErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.LocalDateTime

class ApplicationFailureLanguageSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    context("회원과 토큰 Application failure language에서") {
        test("error code는 명시적인 형식으로 중복 없이 유지된다") {
            val codes = MemberApplicationErrorCode.entries.map { it.code } +
                TokenApplicationErrorCode.entries.map { it.code }

            codes.shouldBeUnique()
            codes.all { it.matches(Regex("[A-Z][A-Z0-9_]+")) } shouldBe true
        }

        test("회원 application error code의 이름, 타입, 메시지는 안정적으로 유지된다") {
            assertContract(
                MemberApplicationErrorCode.entries,
                listOf(
                    Expected("SOCIAL_TYPE_BAD_REQUEST", "MEMBER_SOCIAL_TYPE_INVALID", FrontofficeApplicationErrorType.INVALID_INPUT, "로그인 요청이 유효하지 않습니다."),
                    Expected("AUTHENTICATION_CODE_EXPIRED", "MEMBER_AUTHENTICATION_CODE_EXPIRED", FrontofficeApplicationErrorType.UNAUTHENTICATED, "인가코드가 만료되었습니다"),
                    Expected("SOCIAL_LOGIN_PROVIDER_FAILURE", "SOCIAL_LOGIN_PROVIDER_FAILURE", FrontofficeApplicationErrorType.UPSTREAM_FAILURE, "소셜 로그인 서비스 응답을 처리할 수 없습니다."),
                    Expected("SOCIAL_LOGIN_PROVIDER_UNAVAILABLE", "SOCIAL_LOGIN_PROVIDER_UNAVAILABLE", FrontofficeApplicationErrorType.UPSTREAM_UNAVAILABLE, "소셜 로그인 서비스를 일시적으로 사용할 수 없습니다."),
                    Expected("SOCIAL_LOGIN_PROVIDER_TIMEOUT", "SOCIAL_LOGIN_PROVIDER_TIMEOUT", FrontofficeApplicationErrorType.UPSTREAM_TIMEOUT, "소셜 로그인 서비스 응답이 지연되고 있습니다."),
                    Expected("MEMBER_NOT_FOUND", "MEMBER_NOT_FOUND", FrontofficeApplicationErrorType.NOT_FOUND, "회원이 없습니다"),
                    Expected("USER_NOT_FOUND", "USER_NOT_FOUND", FrontofficeApplicationErrorType.NOT_FOUND, "유저가 없습니다"),
                ),
            )
        }

        test("토큰 application error code의 이름, 타입, 메시지는 안정적으로 유지된다") {
            assertContract(
                TokenApplicationErrorCode.entries,
                listOf(
                    Expected("REFRESH_TOKEN_NOT_FOUND", "REFRESH_TOKEN_NOT_FOUND", FrontofficeApplicationErrorType.NOT_FOUND, "리프레쉬 토큰이 존재하지 않습니다"),
                    Expected("INVALID_REFRESH_TOKEN_ERROR", "INVALID_REFRESH_TOKEN", FrontofficeApplicationErrorType.INVALID_INPUT, "잘못된 리프레쉬 토큰입니다"),
                    Expected("REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR", "REFRESH_TOKEN_MEMBER_ID_MISMATCH", FrontofficeApplicationErrorType.INVALID_INPUT, "리프레쉬 토큰의 사용자 정보가 일치하지 않습니다"),
                    Expected("REFRESH_TOKEN_EXPIRED_ERROR", "REFRESH_TOKEN_EXPIRED", FrontofficeApplicationErrorType.UNAUTHENTICATED, "리프레쉬 토큰이 만료되었습니다"),
                    Expected("REFRESH_TOKEN_SIGNATURE_ERROR", "REFRESH_TOKEN_INVALID_SIGNATURE", FrontofficeApplicationErrorType.INVALID_INPUT, "리프레쉬 토큰의 서명의 잘못 되었습니다"),
                    Expected("UNSUPPORTED_REFRESH_TOKEN_ERROR", "REFRESH_TOKEN_UNSUPPORTED", FrontofficeApplicationErrorType.INVALID_INPUT, "지원하지 않는 리프레쉬 토큰입니다"),
                    Expected("REFRESH_TOKEN_EMPTY_ERROR", "REFRESH_TOKEN_EMPTY", FrontofficeApplicationErrorType.INVALID_INPUT, "리프레쉬 토큰이 비어있습니다"),
                    Expected("UNKNOWN_REFRESH_TOKEN_ERROR", "REFRESH_TOKEN_INTERNAL_ERROR", FrontofficeApplicationErrorType.INTERNAL_ERROR, "알 수 없는 리프레쉬 토큰 오류가 발생했습니다"),
                ),
            )
        }
    }

    test("domain failure는 전체 special mapping과 generic mapping을 보존한다") {
        val dataFormatMessageCodes = setOf<DomainErrorCode>(
            BookingErrorCode.INVALID_PURCHASE_TICKET_COUNT,
            BookingErrorCode.INVALID_REFUND_ACCOUNT,
            BookingErrorCode.PAYMENT_CONFIRMATION_NOT_ALLOWED,
            BookingErrorCode.REFUND_REQUEST_NOT_ALLOWED,
            PerformanceErrorCode.NON_POSITIVE_RUNNING_TIME,
            PerformanceErrorCode.NEGATIVE_SCHEDULE_COUNT,
            ScheduleErrorCode.INVALID_BOOKING_WINDOW,
            ScheduleErrorCode.NEGATIVE_TICKET_COUNT,
            ScheduleErrorCode.NON_POSITIVE_TICKET_COUNT,
        )
        val invalidInputTypeCodes = setOf<DomainErrorCode>(
            BookingErrorCode.PAYMENT_CONFIRMATION_NOT_ALLOWED,
            BookingErrorCode.REFUND_REQUEST_NOT_ALLOWED,
            BookingErrorCode.CONFIRMED_STATUS_CHANGE_NOT_ALLOWED,
            BookingErrorCode.STATUS_TRANSITION_NOT_ALLOWED,
            BookingErrorCode.CANCELLATION_NOT_ALLOWED,
            BookingErrorCode.REFUND_COMPLETION_NOT_ALLOWED,
            BookingErrorCode.DELETION_NOT_ALLOWED,
            ScheduleErrorCode.INSUFFICIENT_TICKETS,
            ScheduleErrorCode.ENDED_SCHEDULE_MODIFICATION_NOT_ALLOWED,
            PerformanceErrorCode.PRICE_UPDATE_NOT_ALLOWED,
        )
        val allDomainCodes: List<DomainErrorCode> =
            BookingErrorCode.entries + PerformanceErrorCode.entries + ScheduleErrorCode.entries

        allDomainCodes.forEach { domainCode ->
            val original = DomainException(domainCode)
            val exception = shouldThrow<FrontofficeApplicationException> {
                translateDomainFailure<Nothing> { throw original }
            }
            val expectedType = when {
                domainCode in invalidInputTypeCodes -> FrontofficeApplicationErrorType.INVALID_INPUT
                domainCode == PerformanceErrorCode.DELETE_NOT_ALLOWED -> FrontofficeApplicationErrorType.FORBIDDEN
                domainCode.type == DomainErrorType.INVALID_INPUT -> FrontofficeApplicationErrorType.INVALID_INPUT
                else -> FrontofficeApplicationErrorType.STATE_CONFLICT
            }
            val expectedMessage = when (domainCode) {
                in dataFormatMessageCodes -> "잘못된 데이터 형식입니다."
                ScheduleErrorCode.TOO_MANY_SCHEDULES -> "공연 회차는 최대 10개까지 추가할 수 있습니다."
                ScheduleErrorCode.ALLOCATED_TICKETS_EXCEED_TOTAL ->
                    "판매된 티켓 수보다 적은 수로 판매할 티켓 매수를 수정할 수 없습니다."
                else -> domainCode.message
            }

            exception.cause shouldBe original
            exception.errorCode.code shouldBe domainCode.code
            exception.errorCode.type shouldBe expectedType
            exception.errorCode.message shouldBe expectedMessage
            exception.message shouldBe expectedMessage
        }
    }

    test("대표 service boundary는 Spring 없이 domain failure를 application failure로 변환한다") {
        val reader = mockk<BookerBookingReader>(relaxed = true)
        every { reader.findByUserId(1L) } returns
            listOf(
                BookerBookingReadModel(
                    userId = 1L,
                    bookingId = 2L,
                    purchaseTicketCount = 1,
                    bookerName = "booker",
                    bookingStatus = "CHECKING_PAYMENT",
                    createdAt = LocalDateTime.of(2026, 1, 1, 12, 0),
                    totalPaymentAmount = null,
                    schedule = BookerBookingScheduleReadModel(
                        scheduleId = 3L,
                        performanceId = 4L,
                        performanceDate = LocalDateTime.of(2026, 1, 2, 12, 0),
                        scheduleNumber = "FIRST",
                    ),
                    performance = BookerBookingPerformanceReadModel(
                        performanceId = 4L,
                        performanceTitle = "performance",
                        performanceVenue = "venue",
                        performanceContact = "contact",
                        bankName = null,
                        accountNumber = null,
                        accountHolder = null,
                        posterImage = "poster",
                        ticketPrice = -1,
                    ),
                ),
            )

        val exception = shouldThrow<FrontofficeApplicationException> {
            GuestBookingQueryService(reader, Clock.systemUTC()).findGuestBookings(1L)
        }

        exception.cause.shouldBeInstanceOf<DomainException>()
        exception.errorCode.code shouldBe PerformanceErrorCode.NEGATIVE_TICKET_PRICE.code
        exception.errorCode.type shouldBe FrontofficeApplicationErrorType.INVALID_INPUT
        exception.errorCode.message shouldBe PerformanceErrorCode.NEGATIVE_TICKET_PRICE.message
    }
})

private fun assertContract(
    actual: List<FrontofficeApplicationErrorCode>,
    expected: List<Expected>,
) {
    expected.map { it.name } shouldBe actual.map { (it as Enum<*>).name }
    expected.map { it.code } shouldBe actual.map { it.code }
    expected.map { it.type } shouldBe actual.map { it.type }
    expected.map { it.message } shouldBe actual.map { it.message }
}

private data class Expected(
    val name: String,
    val code: String,
    val type: FrontofficeApplicationErrorType,
    val message: String,
)
