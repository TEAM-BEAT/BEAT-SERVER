package com.beat.application.frontoffice.booking.booker.experiment

enum class StockContentionStrategy {
    PESSIMISTIC,
    OPTIMISTIC,
    REDIS,
    ATOMIC,
}

enum class StockContentionOutcome {
    ACCEPTED,
    SOLD_OUT,
    CONFLICT_EXHAUSTED,
    LOCK_TIMEOUT,
}

data class StockContentionExperimentResponse(
    val outcome: StockContentionOutcome,
    val bookingId: Long?,
    val attemptCount: Int,
)

data class StockContentionBookingCommand(
    val scheduleId: Long,
    val purchaseTicketCount: Int,
    val bookerName: String,
    val bookerPhoneNumber: String,
)

data class ScheduleBookingMetadata(
    val performanceId: Long,
    val bookingOpen: Boolean,
)

data class StockReservationRequest(
    val scheduleId: Long,
    val performanceId: Long,
    val purchaseTicketCount: Int,
)

data class ScheduleStockState(
    val id: Long,
    val performanceId: Long,
    val performanceDate: java.time.LocalDateTime,
    val bookingCloseAt: java.time.LocalDateTime,
    val totalTicketCount: Int,
    val soldTicketCount: Int,
    val scheduleNumber: String,
    val bookingOpen: Boolean,
    val version: Long?,
) {
    fun canReserve(ticketCount: Int): Boolean =
        bookingOpen && ticketCount > 0 && soldTicketCount <= totalTicketCount - ticketCount
}

data class StockReservationDecision(val outcome: StockContentionOutcome)

class OptimisticReservationConflict : RuntimeException(null, null, false, false)

class StockContentionLockTimeout : RuntimeException(null, null, false, false)

class StockContentionLockUnavailable(cause: Throwable) : RuntimeException(cause)

interface StockContentionReservationStrategy {
    val strategy: StockContentionStrategy

    fun reserve(request: StockReservationRequest): StockReservationDecision

    fun <T> executeWithReservationLock(scheduleId: Long, operation: () -> T): T = operation()
}
