package com.beat.domain.booking.vo

import com.beat.domain.booking.exception.BookingErrorCode
import com.beat.domain.exception.DomainException
import com.beat.domain.sharedkernel.vo.BankName

@ConsistentCopyVisibility
data class RefundAccount
private constructor(
    val bankName: BankName,
    val accountNumber: String,
    val accountHolder: String,
) {
    override fun toString(): String = "RefundAccount(REDACTED)"

    companion object {
        fun of(bankName: BankName?, accountNumber: String?, accountHolder: String?): RefundAccount {
            if (
                bankName == null ||
                    bankName == BankName.NONE ||
                    accountNumber.isNullOrBlank() ||
                    accountHolder.isNullOrBlank()
            ) {
                throw DomainException(BookingErrorCode.INVALID_REFUND_ACCOUNT)
            }
            return RefundAccount(bankName, accountNumber, accountHolder)
        }

        fun fromNullable(
            bankName: BankName?,
            accountNumber: String?,
            accountHolder: String?,
        ): RefundAccount? {
            if (bankName == null && accountNumber == null && accountHolder == null) {
                return null
            }
            return of(bankName, accountNumber, accountHolder)
        }
    }
}
