package com.beat.domain.performance

import com.beat.domain.exception.DomainException
import com.beat.domain.performance.exception.PerformanceErrorCode
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.sharedkernel.vo.BankName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import java.time.LocalDate
import java.time.LocalDateTime

class PerformanceValueObjectSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    context("결제 계좌") {
        test("모든 값이 없으면 계좌가 없는 상태다") {
            PaymentAccount.fromNullable(null, null, null).shouldBeNull()
            PaymentAccount.fromNullable(BankName.NONE, "", " ").shouldBeNull()
        }

        test("은행, 계좌번호, 예금주가 모두 있으면 생성할 수 있다") {
            PaymentAccount.fromNullable(BankName.KAKAOBANK, "123", "holder")!!.bankName shouldBe BankName.KAKAOBANK
        }

        test("일부 값만 있거나 유효하지 않은 값이면 생성할 수 없다") {
            assertDomainError(PerformanceErrorCode.INCOMPLETE_PAYMENT_ACCOUNT) {
                PaymentAccount.fromNullable(BankName.KAKAOBANK, null, "holder")
            }
            shouldThrow<DomainException> { PaymentAccount.of(BankName.KAKAOBANK, " ", "holder") }
            shouldThrow<DomainException> { PaymentAccount.of(BankName.KAKAOBANK, "123", " ") }
            shouldThrow<DomainException> { PaymentAccount.of(BankName.NONE, "123", "holder") }
        }
    }

    context("공연 기간") {
        test("회차들의 가장 이른 날짜와 가장 늦은 날짜를 사용한다") {
            val period = PerformancePeriod.fromPerformanceDateTimes(
                listOf(
                    LocalDateTime.of(2026, 7, 18, 20, 0),
                    LocalDateTime.of(2026, 7, 16, 22, 0),
                    LocalDateTime.of(2026, 7, 17, 19, 0),
                ),
            )

            period.startDate shouldBe LocalDate.of(2026, 7, 16)
            period.endDate shouldBe LocalDate.of(2026, 7, 18)
        }

        test("회차가 없거나 종료일이 시작일보다 빠르면 생성할 수 없다") {
            assertDomainError(PerformanceErrorCode.INVALID_PERFORMANCE_PERIOD) {
                PerformancePeriod.fromDates(emptyList())
            }
            assertDomainError(PerformanceErrorCode.INVALID_PERFORMANCE_PERIOD) {
                PerformancePeriod.of(LocalDate.of(2026, 7, 18), LocalDate.of(2026, 7, 16))
            }
        }
    }

    context("상영 시간과 티켓 가격") {
        test("상영 종료 시각과 구매 금액을 계산한다") {
            RunningTime.of(90).endsAt(LocalDateTime.of(2026, 7, 16, 20, 0)) shouldBe
                LocalDateTime.of(2026, 7, 16, 21, 30)
            TicketPrice.of(20_000).totalFor(3) shouldBe 60_000L
        }

        test("상영 시간은 양수이고 티켓 가격과 구매 수량은 음수가 아니어야 한다") {
            shouldThrow<DomainException> { RunningTime.of(0) }
            shouldThrow<DomainException> { TicketPrice.of(-1) }
            assertDomainError(PerformanceErrorCode.NEGATIVE_TICKET_QUANTITY) {
                TicketPrice.of(20_000).totalFor(-1)
            }
        }
    }
})

private inline fun assertDomainError(expected: PerformanceErrorCode, action: () -> Unit) {
    shouldThrow<DomainException>(action).errorCode shouldBe expected
}
