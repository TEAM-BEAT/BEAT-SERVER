package com.beat.domain.performance.vo

import com.beat.domain.exception.DomainException
import com.beat.domain.performance.exception.PerformanceErrorCode

@ConsistentCopyVisibility
data class TicketPrice private constructor(val amount: Int) {
    fun totalFor(quantity: Int): Long {
        if (quantity < 0) {
            throw DomainException(PerformanceErrorCode.NEGATIVE_TICKET_QUANTITY)
        }
        return amount.toLong() * quantity
    }

    companion object {
        fun of(amount: Int): TicketPrice {
            if (amount < 0) {
                throw DomainException(PerformanceErrorCode.NEGATIVE_TICKET_PRICE)
            }
            return TicketPrice(amount)
        }
    }
}
