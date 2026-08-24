package com.beat.domain.booking

import com.beat.domain.booking.exception.BookingErrorCode
import com.beat.domain.booking.fixture.bookingFixture
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.vo.RefundAccount
import com.beat.domain.exception.DomainException
import com.beat.domain.sharedkernel.vo.BankName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class BookingCreationInvariantSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    context("예매 티켓 수량") {
        test("0 이하이면 생성할 수 없다") {
            shouldFailWith(BookingErrorCode.INVALID_PURCHASE_TICKET_COUNT) {
                booking(purchaseTicketCount = 0)
            }
        }

        test("1장부터 10장까지 허용하고 11장은 거부한다") {
            booking(purchaseTicketCount = 1).purchaseTicketCount shouldBe 1
            booking(purchaseTicketCount = 10).purchaseTicketCount shouldBe 10
            shouldFailWith(BookingErrorCode.PURCHASE_TICKET_COUNT_EXCEEDED) {
                booking(purchaseTicketCount = 11)
            }
        }
    }

    test("무료 예매는 확정되고 유료 또는 미정 금액은 입금 확인 상태로 생성된다") {
        booking(totalPaymentAmount = 0).bookingStatus shouldBe BookingStatus.BOOKING_CONFIRMED
        booking(totalPaymentAmount = 10_000).bookingStatus shouldBe BookingStatus.CHECKING_PAYMENT
        booking(totalPaymentAmount = null).bookingStatus shouldBe BookingStatus.CHECKING_PAYMENT
    }

    test("rehydrate는 저장된 예매 필드를 복원한다") {
        val cancellationDate = LocalDateTime.of(2026, 4, 30, 19, 0)
        val refundAccount = RefundAccount.of(BankName.KAKAOBANK, "111-222", "holder")
        val booking = Booking.rehydrate(
            id = 10L,
            purchaseTicketCount = 2,
            bookerName = "booker",
            bookerPhoneNumber = "010-1234-5678",
            bookingStatus = BookingStatus.BOOKING_CANCELLED,
            createdAt = CREATED_AT,
            cancellationDate = cancellationDate,
            birthDate = "990101",
            password = "1234",
            refundAccount = refundAccount,
            scheduleId = 20L,
            userId = 30L,
        )

        booking.id shouldBe 10L
        booking.purchaseTicketCount shouldBe 2
        booking.bookerName shouldBe "booker"
        booking.bookerPhoneNumber shouldBe "010-1234-5678"
        booking.bookingStatus shouldBe BookingStatus.BOOKING_CANCELLED
        booking.createdAt shouldBe CREATED_AT
        booking.cancellationDate shouldBe cancellationDate
        booking.birthDate shouldBe "990101"
        booking.password shouldBe "1234"
        booking.refundAccount shouldBe refundAccount
        booking.bankName shouldBe BankName.KAKAOBANK
        booking.accountNumber shouldBe "111-222"
        booking.accountHolder shouldBe "holder"
        booking.scheduleId shouldBe 20L
        booking.userId shouldBe 30L
    }
})

private fun booking(
    purchaseTicketCount: Int = 1,
    totalPaymentAmount: Int? = null,
): Booking = bookingFixture(
    purchaseTicketCount = purchaseTicketCount,
    bookerName = "booker",
    bookerPhoneNumber = "010-1234-5678",
    birthDate = "990101",
    password = "1234",
    scheduleId = SCHEDULE_ID,
    userId = USER_ID,
    createdAt = CREATED_AT,
    totalPaymentAmount = totalPaymentAmount,
)

private inline fun shouldFailWith(expected: BookingErrorCode, action: () -> Unit) {
    shouldThrow<DomainException>(action).errorCode shouldBe expected
}

private const val SCHEDULE_ID = 2L
private const val USER_ID = 3L
private val CREATED_AT = LocalDateTime.of(2026, 1, 1, 12, 0)
