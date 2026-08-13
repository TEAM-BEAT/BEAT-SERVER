package com.beat.domain.booking.model

enum class BookingStatus(
    val displayName: String,
) {
    CHECKING_PAYMENT("입금확인중"),
    BOOKING_CONFIRMED("예매 확정"),
    BOOKING_CANCELLED("예매 취소"),
    REFUND_REQUESTED("환불 요청"),
    BOOKING_DELETED("예매 삭제"),
    ;

    fun isInactiveForTicketAllocation(): Boolean = this in inactiveTicketAllocationStatuses

    companion object {
        private val inactiveTicketAllocationStatuses = listOf(BOOKING_CANCELLED, BOOKING_DELETED)

        @JvmStatic
        fun inactiveForTicketAllocation(): List<BookingStatus> = inactiveTicketAllocationStatuses
    }
}
