package com.beat.application.frontoffice.ticket.command

data class TicketUpdateCommand(
    val performanceId: Long,
    val bookingList: List<TicketStatusUpdate>,
)

data class TicketStatusUpdate(
    val bookingId: Long,
    val bookingStatus: TicketBookingStatus,
)

enum class TicketBookingStatus {
    CHECKING_PAYMENT,
    BOOKING_CONFIRMED,
    BOOKING_CANCELLED,
    REFUND_REQUESTED,
    BOOKING_DELETED,
}

data class TicketBookingIdsCommand(
    val performanceId: Long,
    val bookingIds: List<Long>,
)
