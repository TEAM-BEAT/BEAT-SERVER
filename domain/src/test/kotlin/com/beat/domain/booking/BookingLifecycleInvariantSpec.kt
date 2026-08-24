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
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.beTheSameInstanceAs
import java.time.LocalDateTime

class BookingLifecycleInvariantSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    test("미입금 예매 취소는 원본을 보존하고 전달받은 시각으로 불변 복사본을 만든다") {
        val booking = booking()
        val cancelledAt = LocalDateTime.of(2026, 1, 2, 12, 0)

        val cancelled = booking.cancelUnpaidOrFree(cancelledAt)

        cancelled shouldNot beTheSameInstanceAs(booking)
        booking.bookingStatus shouldBe BookingStatus.CHECKING_PAYMENT
        booking.cancellationDate shouldBe null
        cancelled.bookingStatus shouldBe BookingStatus.BOOKING_CANCELLED
        cancelled.cancellationDate shouldBe cancelledAt
    }

    test("취소된 예매 삭제는 기존 취소일을 보존한다") {
        val cancellationDate = LocalDateTime.of(2026, 1, 2, 12, 0)
        val booking = Booking.rehydrate(
            id = 10L,
            purchaseTicketCount = 1,
            bookerName = "booker",
            bookerPhoneNumber = "010-1234-5678",
            bookingStatus = BookingStatus.BOOKING_CANCELLED,
            createdAt = CREATED_AT,
            cancellationDate = cancellationDate,
            birthDate = "990101",
            password = "1234",
            refundAccount = null,
            scheduleId = SCHEDULE_ID,
            userId = USER_ID,
        )

        val deleted = booking.delete()

        deleted shouldNot beTheSameInstanceAs(booking)
        deleted.bookingStatus shouldBe BookingStatus.BOOKING_DELETED
        deleted.cancellationDate shouldBe cancellationDate
    }

    context("예매 삭제") {
        test("취소 또는 삭제 상태만 허용하고 삭제 상태에서는 같은 인스턴스를 반환한다") {
            val cancelled = booking().cancelUnpaidOrFree(CANCELLED_AT)
            val deleted = cancelled.delete()

            deleted.bookingStatus shouldBe BookingStatus.BOOKING_DELETED
            cancelled.delete().bookingStatus shouldBe BookingStatus.BOOKING_DELETED
            deleted.delete() should beTheSameInstanceAs(deleted)
        }

        test("입금 확인 중, 확정, 환불 요청 상태는 삭제할 수 없다") {
            val checking = booking()
            val confirmed = checking.confirmPayment()
            val refundRequested = confirmed.requestRefund(refundAccount())

            shouldFailWith(BookingErrorCode.DELETION_NOT_ALLOWED) { checking.delete() }
            shouldFailWith(BookingErrorCode.DELETION_NOT_ALLOWED) { confirmed.delete() }
            shouldFailWith(BookingErrorCode.DELETION_NOT_ALLOWED) { refundRequested.delete() }
        }
    }

    context("메이커의 예매 삭제") {
        test("미입금과 무료 확정 예매는 취소 후 삭제한다") {
            val deletedAt = LocalDateTime.of(2026, 1, 2, 12, 0)
            val unpaid = booking()
            val free = booking(totalPaymentAmount = 0)

            val deletedUnpaid = unpaid.deleteByMaker(deletedAt)
            val deletedFree = free.deleteByMaker(deletedAt)

            Booking.canDeleteByMaker(BookingStatus.CHECKING_PAYMENT, null) shouldBe true
            Booking.canDeleteByMaker(BookingStatus.BOOKING_CONFIRMED, 0) shouldBe true
            deletedUnpaid.bookingStatus shouldBe BookingStatus.BOOKING_DELETED
            deletedUnpaid.cancellationDate shouldBe deletedAt
            deletedFree.bookingStatus shouldBe BookingStatus.BOOKING_DELETED
            deletedFree.cancellationDate shouldBe deletedAt
        }

        test("유료 확정과 환불 요청 예매는 삭제할 수 없다") {
            val deletedAt = LocalDateTime.of(2026, 1, 2, 12, 0)
            val confirmed = booking(totalPaymentAmount = 10_000).confirmPayment()
            val refundRequested = confirmed.requestRefund(refundAccount())

            Booking.canDeleteByMaker(BookingStatus.BOOKING_CONFIRMED, 10_000) shouldBe false
            Booking.canDeleteByMaker(BookingStatus.REFUND_REQUESTED, 0) shouldBe false
            shouldFailWith(BookingErrorCode.DELETION_NOT_ALLOWED) { confirmed.deleteByMaker(deletedAt) }
            shouldFailWith(BookingErrorCode.DELETION_NOT_ALLOWED) { refundRequested.deleteByMaker(deletedAt) }
        }
    }

    test("환불 요청은 원본을 보존하고 계좌와 상태를 담은 불변 복사본을 만든다") {
        val booking = booking()
        val confirmed = booking.confirmPayment()
        val account = refundAccount()

        val requested = confirmed.requestRefund(account)

        requested shouldNot beTheSameInstanceAs(confirmed)
        booking.refundAccount shouldBe null
        booking.bookingStatus shouldBe BookingStatus.CHECKING_PAYMENT
        confirmed.bookingStatus shouldBe BookingStatus.BOOKING_CONFIRMED
        requested.refundAccount shouldBe account
        requested.bookingStatus shouldBe BookingStatus.REFUND_REQUESTED
    }

    test("입금 확인 중 예매를 확정하고 동일 상태 전환은 같은 인스턴스를 반환한다") {
        val confirmed = booking().transitionTo(BookingStatus.BOOKING_CONFIRMED)

        confirmed.bookingStatus shouldBe BookingStatus.BOOKING_CONFIRMED
        confirmed.transitionTo(BookingStatus.BOOKING_CONFIRMED) should beTheSameInstanceAs(confirmed)
    }

    test("확정된 예매를 취소 상태로 전환할 수 없다") {
        val confirmed = booking().confirmPayment()

        shouldFailWith(BookingErrorCode.CONFIRMED_STATUS_CHANGE_NOT_ALLOWED) {
            confirmed.transitionTo(BookingStatus.BOOKING_CANCELLED)
        }
    }

    test("결제 확정은 확정 상태에서 멱등이고 종료 상태에서는 거부한다") {
        val confirmed = booking().confirmPayment()
        val cancelled = booking().cancelUnpaidOrFree(CANCELLED_AT)

        confirmed.confirmPayment() should beTheSameInstanceAs(confirmed)
        shouldFailWith(BookingErrorCode.PAYMENT_CONFIRMATION_NOT_ALLOWED) { cancelled.confirmPayment() }
    }

    context("환불 요청 상태") {
        test("같은 계좌 요청은 멱등이고 다른 계좌 요청은 거부한다") {
            val account = refundAccount()
            val requested = booking().requestRefund(account)

            requested.requestRefund(account) should beTheSameInstanceAs(requested)
            requested.bookingStatus shouldBe BookingStatus.REFUND_REQUESTED
            shouldFailWith(BookingErrorCode.REFUND_REQUEST_NOT_ALLOWED) {
                requested.requestRefund(RefundAccount.of(BankName.KAKAOBANK, "999", "other"))
            }
        }

        test("무료 확정 예매는 환불을 요청할 수 없다") {
            shouldFailWith(BookingErrorCode.REFUND_REQUEST_NOT_ALLOWED) {
                booking(totalPaymentAmount = 0).requestRefund(refundAccount())
            }
        }
    }

    context("일반 취소") {
        test("유료 확정과 환불 요청 예매는 취소할 수 없다") {
            val confirmed = booking(totalPaymentAmount = 10_000).confirmPayment()
            val refundRequested = confirmed.requestRefund(refundAccount())

            shouldFailWith(BookingErrorCode.CANCELLATION_NOT_ALLOWED) {
                confirmed.cancelUnpaidOrFree(CANCELLED_AT)
            }
            shouldFailWith(BookingErrorCode.CANCELLATION_NOT_ALLOWED) {
                refundRequested.cancelUnpaidOrFree(CANCELLED_AT)
            }
        }

        test("무료 확정 예매는 취소할 수 있다") {
            val cancelled = booking(totalPaymentAmount = 0).cancelUnpaidOrFree(CANCELLED_AT)

            cancelled.bookingStatus shouldBe BookingStatus.BOOKING_CANCELLED
            cancelled.cancellationDate shouldBe CANCELLED_AT
        }
    }

    test("환불 완료는 환불 요청 상태만 취소로 전환하고 취소 상태에서는 멱등이다") {
        val requested = booking(totalPaymentAmount = 10_000)
            .confirmPayment()
            .requestRefund(refundAccount())
        val completedAt = LocalDateTime.of(2026, 1, 2, 12, 0)
        val completed = requested.completeRefund(completedAt)

        completed.bookingStatus shouldBe BookingStatus.BOOKING_CANCELLED
        completed.cancellationDate shouldBe completedAt
        completed.completeRefund(completedAt.plusDays(1)) should beTheSameInstanceAs(completed)
        shouldFailWith(BookingErrorCode.REFUND_COMPLETION_NOT_ALLOWED) {
            booking(totalPaymentAmount = 10_000).confirmPayment().completeRefund(completedAt)
        }
    }

    test("반복 취소는 최초 취소 시각과 비활성 티켓 상태를 보존한다") {
        val cancelled = booking().cancelUnpaidOrFree(CANCELLED_AT)
        val repeated = cancelled.cancelUnpaidOrFree(CANCELLED_AT.plusDays(1))

        repeated should beTheSameInstanceAs(cancelled)
        repeated.cancellationDate shouldBe CANCELLED_AT
        repeated.hasActiveTicketAllocation() shouldBe false
    }
})

private fun booking(totalPaymentAmount: Int? = null): Booking = bookingFixture(
    totalPaymentAmount = totalPaymentAmount,
)

private fun refundAccount(): RefundAccount = RefundAccount.of(
    BankName.NH_NONGHYUP,
    "123-456",
    "holder",
)

private inline fun shouldFailWith(expected: BookingErrorCode, action: () -> Unit) {
    shouldThrow<DomainException>(action).errorCode shouldBe expected
}

private const val SCHEDULE_ID = 2L
private const val USER_ID = 3L
private val CREATED_AT = LocalDateTime.of(2026, 1, 1, 12, 0)
private val CANCELLED_AT = LocalDateTime.of(2026, 1, 2, 12, 0)
