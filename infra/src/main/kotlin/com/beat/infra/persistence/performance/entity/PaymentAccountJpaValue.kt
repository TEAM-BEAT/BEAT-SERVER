package com.beat.infra.persistence.performance.entity

import com.beat.domain.sharedkernel.vo.BankName
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
class PaymentAccountJpaValue(
    bankName: BankName,
    accountNumber: String,
    accountHolder: String,
) {
    @Enumerated(EnumType.STRING)
    @Column(name = "bank_name")
    var bankName: BankName = bankName
        protected set

    @Column(name = "account_number")
    var accountNumber: String = accountNumber
        protected set

    @Column(name = "account_holder")
    var accountHolder: String = accountHolder
        protected set
}
