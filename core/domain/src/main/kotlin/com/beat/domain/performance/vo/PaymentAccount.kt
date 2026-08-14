package com.beat.domain.performance.vo

import com.beat.domain.exception.DomainException
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.domain.performance.exception.PerformanceErrorCode

@ConsistentCopyVisibility
data class PaymentAccount private constructor(
    val bankName: BankName,
    val accountNumber: String,
    val accountHolder: String,
) {
    override fun toString(): String = "PaymentAccount(REDACTED)"

    companion object {
        @JvmStatic
        fun of(bankName: BankName, accountNumber: String, accountHolder: String): PaymentAccount {
            if (bankName == BankName.NONE || accountNumber.isBlank() || accountHolder.isBlank()) {
                throw DomainException(PerformanceErrorCode.INCOMPLETE_PAYMENT_ACCOUNT)
            }
            return PaymentAccount(bankName, accountNumber, accountHolder)
        }

        @JvmStatic
        fun fromNullable(
            bankName: BankName?,
            accountNumber: String?,
            accountHolder: String?,
        ): PaymentAccount? {
            val normalizedBankName = bankName?.takeUnless { it == BankName.NONE }
            val normalizedAccountNumber = accountNumber?.takeUnless(String::isBlank)
            val normalizedAccountHolder = accountHolder?.takeUnless(String::isBlank)

            if (normalizedBankName == null && normalizedAccountNumber == null && normalizedAccountHolder == null) {
                return null
            }
            if (normalizedBankName == null || normalizedAccountNumber == null || normalizedAccountHolder == null) {
                throw DomainException(PerformanceErrorCode.INCOMPLETE_PAYMENT_ACCOUNT)
            }
            return of(normalizedBankName, normalizedAccountNumber, normalizedAccountHolder)
        }
    }
}
