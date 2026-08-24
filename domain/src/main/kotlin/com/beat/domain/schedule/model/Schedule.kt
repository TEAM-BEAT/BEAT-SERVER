package com.beat.domain.schedule.model

import com.beat.domain.exception.DomainException
import com.beat.domain.performance.model.Performance
import com.beat.domain.schedule.exception.ScheduleErrorCode
import com.beat.domain.sharedkernel.model.AggregateRoot
import java.time.LocalDateTime

class Schedule private constructor(
    private val scheduleId: Id?,
    val performanceDate: LocalDateTime,
    val bookingCloseAt: LocalDateTime,
    val totalTicketCount: Int,
    val allocatedTicketCount: Int,
    val scheduleNumber: ScheduleNumber,
    private val linkedPerformanceId: Performance.Id,
) : AggregateRoot {
    val id: Long?
        get() = scheduleId?.value

    val performanceId: Long
        get() = linkedPerformanceId.value

    val availableTicketCount: Int
        get() = totalTicketCount - allocatedTicketCount

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Schedule) return false
        return scheduleId != null && scheduleId == other.scheduleId
    }

    override fun hashCode(): Int = scheduleId?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "Schedule(id=$id)"

    fun canPurchase(purchaseTicketCount: Int): Boolean =
        purchaseTicketCount > 0 && availableTicketCount >= purchaseTicketCount

    fun belongsTo(performanceId: Long): Boolean = linkedPerformanceId.value == performanceId

    fun update(
        performanceDate: LocalDateTime,
        bookingCloseAt: LocalDateTime,
        totalTicketCount: Int,
        scheduleNumber: ScheduleNumber,
    ): Schedule {
        validateBookingWindow(performanceDate, bookingCloseAt)
        validateTicketCounts(totalTicketCount, allocatedTicketCount)

        return withState(
            performanceDate = performanceDate,
            bookingCloseAt = bookingCloseAt,
            totalTicketCount = totalTicketCount,
            scheduleNumber = scheduleNumber
        )
    }

    fun reserveTickets(count: Int): Schedule {
        validatePositiveTicketCount(count)
        if (!canPurchase(count)) {
            throw DomainException(ScheduleErrorCode.INSUFFICIENT_TICKETS)
        }
        return withState(allocatedTicketCount = allocatedTicketCount + count)
    }

    fun releaseTickets(count: Int): Schedule {
        validatePositiveTicketCount(count)
        if (allocatedTicketCount < count) {
            throw DomainException(ScheduleErrorCode.EXCESS_TICKET_DELETE)
        }
        return withState(allocatedTicketCount = allocatedTicketCount - count)
    }

    fun updateScheduleNumber(scheduleNumber: ScheduleNumber): Schedule = withState(scheduleNumber = scheduleNumber)

    fun updateBookingCloseAt(bookingCloseAt: LocalDateTime): Schedule {
        validateBookingWindow(performanceDate, bookingCloseAt)
        return withState(bookingCloseAt = bookingCloseAt)
    }

    fun reschedule(
        performanceDate: LocalDateTime,
        bookingCloseAt: LocalDateTime,
        totalTicketCount: Int,
        scheduleNumber: ScheduleNumber,
        now: LocalDateTime,
    ): Schedule {
        if (this.performanceDate.isBefore(now)) {
            throw DomainException(ScheduleErrorCode.ENDED_SCHEDULE_MODIFICATION_NOT_ALLOWED)
        }
        validateNotPast(performanceDate, now)
        return update(performanceDate, bookingCloseAt, totalTicketCount, scheduleNumber)
    }

    private fun withState(
        performanceDate: LocalDateTime = this.performanceDate,
        bookingCloseAt: LocalDateTime = this.bookingCloseAt,
        totalTicketCount: Int = this.totalTicketCount,
        allocatedTicketCount: Int = this.allocatedTicketCount,
        scheduleNumber: ScheduleNumber = this.scheduleNumber,
    ): Schedule = Schedule(
        scheduleId = scheduleId,
        performanceDate = performanceDate,
        bookingCloseAt = bookingCloseAt,
        totalTicketCount = totalTicketCount,
        allocatedTicketCount = allocatedTicketCount,
        scheduleNumber = scheduleNumber,
        linkedPerformanceId = linkedPerformanceId,
    )

    @JvmInline
    value class Id private constructor(val value: Long) {
        companion object {
            fun from(value: Long): Id = Id(value)

            fun fromNullable(value: Long?): Id? = value?.let(::from)
        }
    }

    companion object {
        fun create(
            performanceDate: LocalDateTime,
            bookingCloseAt: LocalDateTime,
            totalTicketCount: Int,
            scheduleNumber: ScheduleNumber,
            performanceId: Long,
        ): Schedule {
            validateBookingWindow(performanceDate, bookingCloseAt)
            validateTicketCounts(totalTicketCount, 0)

            return Schedule(
                scheduleId = null,
                performanceDate = performanceDate,
                bookingCloseAt = bookingCloseAt,
                totalTicketCount = totalTicketCount,
                allocatedTicketCount = 0,
                scheduleNumber = scheduleNumber,
                linkedPerformanceId = Performance.Id.from(performanceId)
            )
        }

        fun createUpcoming(
            performanceDate: LocalDateTime,
            bookingCloseAt: LocalDateTime,
            totalTicketCount: Int,
            scheduleNumber: ScheduleNumber,
            performanceId: Long,
            now: LocalDateTime,
        ): Schedule {
            validateNotPast(performanceDate, now)
            return create(performanceDate, bookingCloseAt, totalTicketCount, scheduleNumber, performanceId)
        }

        fun rehydrate(
            id: Long?,
            performanceDate: LocalDateTime,
            bookingCloseAt: LocalDateTime,
            totalTicketCount: Int,
            allocatedTicketCount: Int,
            scheduleNumber: ScheduleNumber,
            performanceId: Long,
        ): Schedule {
            validateBookingWindow(performanceDate, bookingCloseAt)
            validateTicketCounts(totalTicketCount, allocatedTicketCount)

            return Schedule(
                scheduleId = Id.fromNullable(id),
                performanceDate = performanceDate,
                bookingCloseAt = bookingCloseAt,
                totalTicketCount = totalTicketCount,
                allocatedTicketCount = allocatedTicketCount,
                scheduleNumber = scheduleNumber,
                linkedPerformanceId = Performance.Id.from(performanceId)
            )
        }

        private fun validateBookingWindow(performanceDate: LocalDateTime, bookingCloseAt: LocalDateTime) {
            if (bookingCloseAt.isBefore(performanceDate)) {
                throw DomainException(ScheduleErrorCode.INVALID_BOOKING_WINDOW)
            }
        }

        private fun validateTicketCounts(totalTicketCount: Int, allocatedTicketCount: Int) {
            if (totalTicketCount < 0 || allocatedTicketCount < 0) {
                throw DomainException(ScheduleErrorCode.NEGATIVE_TICKET_COUNT)
            }
            if (allocatedTicketCount > totalTicketCount) {
                throw DomainException(ScheduleErrorCode.ALLOCATED_TICKETS_EXCEED_TOTAL)
            }
        }

        private fun validatePositiveTicketCount(ticketCount: Int) {
            if (ticketCount <= 0) {
                throw DomainException(ScheduleErrorCode.NON_POSITIVE_TICKET_COUNT)
            }
        }

        private fun validateNotPast(performanceDate: LocalDateTime, now: LocalDateTime) {
            if (performanceDate.isBefore(now)) {
                throw DomainException(ScheduleErrorCode.PAST_SCHEDULE_NOT_ALLOWED)
            }
        }
    }
}
