package com.beat.application.frontoffice.booking.booker.query

import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.booking.booker.BookingHistoryPerformanceSnapshot
import com.beat.application.frontoffice.booking.booker.BookingHistoryReadPort
import com.beat.application.frontoffice.booking.booker.BookingHistoryScheduleSnapshot
import com.beat.application.frontoffice.booking.booker.BookingHistorySnapshot
import com.beat.application.frontoffice.booking.booker.toResult
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.fixture.frontofficeMemberFixture
import com.beat.domain.member.repository.MemberRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class BookerBookingHistoryQuerySpec :
    FunSpec({
        isolationMode = IsolationMode.SingleInstance

        test("게스트 예매 조회는 reader 순서와 저장된 결제 금액을 보존하고 미저장 금액만 현재 가격으로 계산한다") {
            val reader = mockk<BookingHistoryReadPort>(relaxed = true)
            every { reader.findByUserId(7L) } returns
                listOf(
                    booking(
                        1L,
                        10L,
                        24_000,
                        schedule(10L, 100L, "FIRST"),
                        performance(100L, 15_000),
                    ),
                    booking(
                        2L,
                        11L,
                        null,
                        schedule(11L, 100L, "SECOND"),
                        performance(100L, 20_000),
                    ),
                )

            val results = reader.findByUserId(7L).map { it.toResult(LocalDate.now(FIXED_CLOCK)) }

            results.map { it.scheduleId } shouldBe listOf(10L, 11L)
            results[0].totalPaymentAmount shouldBe 24_000
            results[1].totalPaymentAmount shouldBe 40_000
            results[0].dueDate shouldBe 9
        }

        test("회원 예매 조회는 회원의 userId로 authoritative booking을 조회한다") {
            val memberRepository = mockk<MemberRepository>(relaxed = true)
            val reader = mockk<BookingHistoryReadPort>(relaxed = true)
            val service = MemberBookingQueryService(memberRepository, reader, FIXED_CLOCK)
            val member = frontofficeMemberFixture()
            every { memberRepository.findById(1L) } returns member
            every { reader.findByUserId(7L) } returns
                listOf(
                    booking(
                        1L,
                        10L,
                        20_000,
                        schedule(10L, 100L, "FIRST"),
                        performance(100L, 20_000),
                    )
                )

            val result = service.findMemberBookings(1L).first()

            result.userId shouldBe 7L
            result.scheduleId shouldBe 10L
        }

        test("연관 회차 projection이 없으면 안정적인 회차 없음 application error를 반환한다") {
            val reader = mockk<BookingHistoryReadPort>(relaxed = true)
            every { reader.findByUserId(7L) } returns listOf(booking(1L, 10L, null, null, null))

            val exception =
                shouldThrow<FrontofficeApplicationException> {
                    reader.findByUserId(7L).map { it.toResult(LocalDate.now(FIXED_CLOCK)) }
                }

            exception.errorCode shouldBe BookingApplicationErrorCode.SCHEDULE_NOT_FOUND
            exception.errorCode.code shouldBe "SCHEDULE_NOT_FOUND"
            exception.errorCode.message shouldBe "해당 회차를 찾을 수 없습니다."
        }

        test("연관 공연 projection이 없으면 안정적인 공연 없음 application error를 반환한다") {
            val reader = mockk<BookingHistoryReadPort>(relaxed = true)
            every { reader.findByUserId(7L) } returns
                listOf(booking(1L, 10L, null, schedule(10L, 100L, "FIRST"), null))

            val exception =
                shouldThrow<FrontofficeApplicationException> {
                    reader.findByUserId(7L).map { it.toResult(LocalDate.now(FIXED_CLOCK)) }
                }

            exception.errorCode shouldBe BookingApplicationErrorCode.PERFORMANCE_NOT_FOUND
            exception.errorCode.code shouldBe "PERFORMANCE_NOT_FOUND"
            exception.errorCode.message shouldBe "해당 공연 정보를 찾을 수 없습니다."
        }
    })

private fun booking(
    bookingId: Long,
    scheduleId: Long,
    amount: Int?,
    schedule: BookingHistoryScheduleSnapshot?,
    performance: BookingHistoryPerformanceSnapshot?,
): BookingHistorySnapshot =
    BookingHistorySnapshot(
        userId = 7L,
        bookingId = bookingId,
        purchaseTicketCount = 2,
        bookerName = "booker",
        bookingStatus = "CHECKING_PAYMENT",
        createdAt = LocalDateTime.of(2026, 1, 1, 12, 0),
        totalPaymentAmount = amount,
        schedule = schedule,
        performance = performance,
    )

private fun schedule(
    scheduleId: Long,
    performanceId: Long,
    number: String,
): BookingHistoryScheduleSnapshot =
    BookingHistoryScheduleSnapshot(
        scheduleId = scheduleId,
        performanceId = performanceId,
        performanceDate = LocalDateTime.of(2026, 1, 10, 18, 0),
        scheduleNumber = number,
    )

private fun performance(
    performanceId: Long,
    ticketPrice: Int,
): BookingHistoryPerformanceSnapshot =
    BookingHistoryPerformanceSnapshot(
        performanceId = performanceId,
        performanceTitle = "공연",
        performanceVenue = "공연장",
        performanceContact = "010-0000-0000",
        bankName = "KAKAOBANK",
        accountNumber = "계좌",
        accountHolder = "예금주",
        posterImage = "poster.png",
        ticketPrice = ticketPrice,
    )

private val FIXED_CLOCK: Clock =
    Clock.fixed(
        Instant.parse("2026-01-01T00:00:00Z"),
        ZoneOffset.UTC,
    )
