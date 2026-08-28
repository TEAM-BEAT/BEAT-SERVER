package com.beat.domain.booking

import com.beat.domain.booking.exception.BookingErrorCode
import com.beat.domain.booking.vo.RefundAccount
import com.beat.domain.exception.DomainException
import com.beat.domain.sharedkernel.vo.BankName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class RefundAccountSpec :
    FunSpec({
        isolationMode = IsolationMode.SingleInstance

        test("NONE 은행은 환불 계좌로 사용할 수 없다") {
            shouldFailWithInvalidAccount {
                RefundAccount.of(BankName.NONE, "111-222", "holder")
            }
        }

        test("은행, 계좌번호, 예금주 중 일부가 null이거나 비어 있으면 거부한다") {
            shouldFailWithInvalidAccount {
                RefundAccount.fromNullable(BankName.KAKAOBANK, null, "holder")
            }
            shouldFailWithInvalidAccount {
                RefundAccount.of(BankName.KAKAOBANK, " ", "holder")
            }
            shouldFailWithInvalidAccount {
                RefundAccount.of(BankName.KAKAOBANK, "111-222", " ")
            }
            shouldFailWithInvalidAccount {
                RefundAccount.of(null, "111-222", "holder")
            }
        }

        test("모든 값이 null이면 fromNullable은 null을 반환한다") {
            RefundAccount.fromNullable(null, null, null).shouldBeNull()
        }
    })

private inline fun shouldFailWithInvalidAccount(action: () -> Unit) {
    shouldThrow<DomainException>(action).errorCode shouldBe BookingErrorCode.INVALID_REFUND_ACCOUNT
}
