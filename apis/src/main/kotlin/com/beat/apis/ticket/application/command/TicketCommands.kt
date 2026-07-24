package com.beat.apis.ticket.application.command

@ConsistentCopyVisibility
data class TicketUpdateCommand private constructor(
    val performanceId: Long,
    val bookingList: List<TicketStatusUpdate>,
) {
    companion object {
        @JvmStatic
        fun of(performanceId: Long, bookingList: List<TicketStatusUpdate>): TicketUpdateCommand =
            TicketUpdateCommand(performanceId, bookingList)
    }
}

@ConsistentCopyVisibility
data class TicketStatusUpdate private constructor(
    val bookingId: Long,
    val bookingStatus: TicketBookingStatus,
) {
    companion object {
        @JvmStatic
        fun of(bookingId: Long, bookingStatus: TicketBookingStatus): TicketStatusUpdate =
            TicketStatusUpdate(bookingId, bookingStatus)
    }
}

enum class TicketBookingStatus {
    CHECKING_PAYMENT,
    BOOKING_CONFIRMED,
    BOOKING_CANCELLED,
    REFUND_REQUESTED,
    BOOKING_DELETED,
}

@ConsistentCopyVisibility
data class TicketBookingIdsCommand private constructor(
    val performanceId: Long,
    val bookingIds: List<Long>,
) {
    companion object {
        @JvmStatic
        fun of(performanceId: Long, bookingIds: List<Long>): TicketBookingIdsCommand =
            TicketBookingIdsCommand(performanceId, bookingIds)
    }
}
