package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.exception.BookingApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.domain.booking.exception.BookingErrorCode
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class BookingLifecycleCommandSpec :
    FunSpec({
        isolationMode = IsolationMode.SingleInstance

        test("이미 취소된 예매를 반복 취소해도 재고를 다시 해제하지 않는다") {
            val bookingRepository = bookingRepositoryWithSavePassthrough()
            val scheduleRepository = scheduleRepositoryWithSavePassthrough()
            val cancelled =
                booking(BookingStatus.BOOKING_CANCELLED, LocalDateTime.of(2026, 1, 2, 12, 0))
            every { bookingRepository.findById(1L) } returns cancelled
            every { bookingRepository.lockById(1L) } returns cancelled

            cancellationService(bookingRepository, scheduleRepository)
                .cancelBooking(MEMBER_ACTOR, BookingCancelCommand.from(1L))

            verify { bookingRepository.lockById(1L) }
            verify(exactly = 0) { scheduleRepository.lockById(any()) }
            verify(exactly = 0) { scheduleRepository.save(any()) }
        }

        test("active 예매 취소는 회차를 잠그고 티켓 재고를 한 번 해제한다") {
            val bookingRepository = bookingRepositoryWithSavePassthrough()
            val scheduleRepository = scheduleRepositoryWithSavePassthrough()
            val active = booking(BookingStatus.CHECKING_PAYMENT, null)
            val schedule = schedule()
            every { bookingRepository.findById(1L) } returns active
            every { bookingRepository.lockById(1L) } returns active
            every { scheduleRepository.lockById(10L) } returns schedule
            val savedScheduleSlot = slot<Schedule>()
            every { scheduleRepository.save(capture(savedScheduleSlot)) } answers
                {
                    savedScheduleSlot.captured
                }

            cancellationService(bookingRepository, scheduleRepository)
                .cancelBooking(MEMBER_ACTOR, BookingCancelCommand.from(1L))

            savedScheduleSlot.captured.allocatedTicketCount shouldBe 1
        }

        test("active 예매의 회차가 없으면 안정적인 회차 없음 error를 반환한다") {
            val bookingRepository = bookingRepositoryWithSavePassthrough()
            val scheduleRepository = scheduleRepositoryWithSavePassthrough()
            every { bookingRepository.findById(1L) } returns
                booking(BookingStatus.CHECKING_PAYMENT, null)
            every { scheduleRepository.lockById(10L) } returns null

            val exception =
                shouldThrow<FrontofficeApplicationException> {
                    cancellationService(bookingRepository, scheduleRepository)
                        .cancelBooking(MEMBER_ACTOR, BookingCancelCommand.from(1L))
                }

            exception.errorCode.code shouldBe "SCHEDULE_NOT_FOUND"
            exception.errorCode.message shouldBe "해당 회차를 찾을 수 없습니다."
        }

        test("다른 사용자의 예매는 소유 여부를 노출하지 않고 아무 상태도 잠그거나 변경하지 않는다") {
            val bookingRepository = bookingRepositoryWithSavePassthrough()
            val scheduleRepository = scheduleRepositoryWithSavePassthrough()
            every { bookingRepository.findById(1L) } returns
                booking(BookingStatus.CHECKING_PAYMENT, null)

            val exception =
                shouldThrow<FrontofficeApplicationException> {
                    cancellationService(bookingRepository, scheduleRepository)
                        .cancelBooking(
                            BookingActorCommand(999L, null),
                            BookingCancelCommand.from(1L),
                        )
                }

            exception.errorCode shouldBe BookingApplicationErrorCode.NO_BOOKING_FOUND
            verify(exactly = 0) { bookingRepository.lockById(any()) }
            verify { scheduleRepository wasNot Called }
        }

        test("확정 예매 취소가 거부되면 booking과 schedule을 저장하지 않는다") {
            val bookingRepository = bookingRepositoryWithSavePassthrough()
            val scheduleRepository = scheduleRepositoryWithSavePassthrough()
            every { bookingRepository.findById(1L) } returns
                booking(BookingStatus.BOOKING_CONFIRMED, null)
            every { bookingRepository.lockById(1L) } returns
                booking(BookingStatus.BOOKING_CONFIRMED, null)
            every { scheduleRepository.lockById(10L) } returns schedule()

            val exception =
                shouldThrow<FrontofficeApplicationException> {
                    cancellationService(bookingRepository, scheduleRepository)
                        .cancelBooking(MEMBER_ACTOR, BookingCancelCommand.from(1L))
                }

            exception.errorCode.code shouldBe BookingErrorCode.CANCELLATION_NOT_ALLOWED.code
            exception.errorCode.type shouldBe FrontofficeApplicationErrorType.INVALID_INPUT
            exception.errorCode.message shouldBe BookingErrorCode.CANCELLATION_NOT_ALLOWED.message
            verify(exactly = 0) { bookingRepository.save(any()) }
            verify(exactly = 0) { scheduleRepository.save(any()) }
        }

        test("입금 확인 중 예매는 환불 계좌를 저장하고 회차 재고를 변경하지 않는다") {
            val bookingRepository = bookingRepositoryWithSavePassthrough()
            val scheduleRepository = scheduleRepositoryWithSavePassthrough()
            every { bookingRepository.lockById(1L) } returns
                booking(BookingStatus.CHECKING_PAYMENT, null)

            val result =
                cancellationService(bookingRepository, scheduleRepository)
                    .refundBooking(
                        MEMBER_ACTOR,
                        BookingRefundCommand.of(1L, "KAKAOBANK", "123-456", "holder"),
                    )

            result.bookingStatus shouldBe BookingStatus.REFUND_REQUESTED.name
            verify { scheduleRepository wasNot Called }
        }
    })

private fun cancellationService(
    bookingRepository: BookingRepository,
    scheduleRepository: ScheduleRepository,
): BookingCancellationCommandService =
    BookingCancellationCommandService(
        bookingRepository,
        FIXED_CLOCK,
        scheduleRepository,
        GuestBookingSessionManager(mockk(relaxed = true)),
    )

private fun booking(status: BookingStatus, cancelledAt: LocalDateTime?): Booking =
    Booking.rehydrate(
        id = 1L,
        purchaseTicketCount = 1,
        bookerName = "booker",
        bookerPhoneNumber = "010-1234-5678",
        bookingStatus = status,
        createdAt = LocalDateTime.of(2026, 1, 1, 12, 0),
        cancellationDate = cancelledAt,
        birthDate = "990101",
        password = "1234",
        refundAccount = null,
        scheduleId = 10L,
        userId = 20L,
        totalPaymentAmount = 10_000,
    )

private fun schedule(): Schedule {
    val performanceDate = LocalDateTime.of(2026, 2, 1, 18, 0)
    return Schedule.rehydrate(
        id = 10L,
        performanceDate = performanceDate,
        bookingCloseAt = performanceDate.plusHours(2),
        totalTicketCount = 10,
        allocatedTicketCount = 2,
        scheduleNumber = ScheduleNumber.FIRST,
        performanceId = 30L,
    )
}

private val FIXED_CLOCK: Clock =
    Clock.fixed(
        Instant.parse("2026-01-02T12:00:00Z"),
        ZoneOffset.UTC,
    )
private val MEMBER_ACTOR = BookingActorCommand(20L, null)

private fun bookingRepositoryWithSavePassthrough(): BookingRepository =
    mockk(relaxed = true) {
        every { save(any()) } answers { firstArg() }
    }

private fun scheduleRepositoryWithSavePassthrough(): ScheduleRepository =
    mockk(relaxed = true) {
        every { save(any()) } answers { firstArg() }
    }
