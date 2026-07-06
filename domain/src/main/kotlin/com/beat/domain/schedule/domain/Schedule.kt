package com.beat.domain.schedule.domain

import com.beat.domain.performance.domain.Performance
import com.beat.domain.schedule.exception.ScheduleErrorCode
import com.beat.global.support.exception.BadRequestException
import com.beat.global.support.exception.ConflictException
import java.time.LocalDateTime

@ConsistentCopyVisibility
data class Schedule private constructor(
    private val scheduleId: Id?,
    private val performanceDate: LocalDateTime,
    private val bookingCloseAt: LocalDateTime,
    private val totalTicketCount: Int,
    private val soldTicketCount: Int,
    private val scheduleNumber: ScheduleNumber,
    private val linkedPerformanceId: Performance.Id,
) {
    fun getId(): Long? = scheduleId?.value

    fun getPerformanceId(): Long = linkedPerformanceId.value

    fun getPerformanceDate(): LocalDateTime = performanceDate

    fun getBookingCloseAt(): LocalDateTime = bookingCloseAt

    fun getTotalTicketCount(): Int = totalTicketCount

    fun getSoldTicketCount(): Int = soldTicketCount

    fun getScheduleNumber(): ScheduleNumber = scheduleNumber

    fun update(
        performanceDate: LocalDateTime,
        bookingCloseAt: LocalDateTime,
        totalTicketCount: Int,
        scheduleNumber: ScheduleNumber,
    ): Schedule {
        validateTicketCounts(totalTicketCount, soldTicketCount)
        validateBookingWindow(performanceDate, bookingCloseAt)

        return copy(
            performanceDate = performanceDate,
            bookingCloseAt = bookingCloseAt,
            totalTicketCount = totalTicketCount,
            scheduleNumber = scheduleNumber
        )
    }

    fun increaseSoldTicketCount(count: Int): Schedule {
        validatePositiveCount(count)

        val updatedSoldTicketCount = soldTicketCount + count
        if (updatedSoldTicketCount > totalTicketCount) {
            throw ConflictException(ScheduleErrorCode.INSUFFICIENT_TICKETS)
        }

        return copy(soldTicketCount = updatedSoldTicketCount)
    }

    fun decreaseSoldTicketCount(count: Int): Schedule {
        validatePositiveCount(count)

        if (soldTicketCount < count) {
            throw ConflictException(ScheduleErrorCode.EXCESS_TICKET_DELETE)
        }
        return copy(soldTicketCount = soldTicketCount - count)
    }

    fun updateScheduleNumber(scheduleNumber: ScheduleNumber): Schedule = copy(scheduleNumber = scheduleNumber)

    fun updateBookingCloseAt(bookingCloseAt: LocalDateTime): Schedule {
        validateBookingWindow(performanceDate, bookingCloseAt)
        return copy(bookingCloseAt = bookingCloseAt)
    }

    private fun validatePositiveCount(count: Int) {
        if (count <= 0) {
            throw BadRequestException(ScheduleErrorCode.INVALID_DATA_FORMAT)
        }
    }

    @JvmInline
    value class Id private constructor(val value: Long) {
        companion object {
            @JvmStatic
            fun from(value: Long): Id = Id(value)

            @JvmStatic
            fun fromNullable(value: Long?): Id? = value?.let(::from)
        }
    }

    companion object {
        @JvmStatic
        fun create(
            performanceDate: LocalDateTime,
            bookingCloseAt: LocalDateTime,
            totalTicketCount: Int,
            scheduleNumber: ScheduleNumber,
            performanceId: Long,
        ): Schedule {
            validateTicketCounts(totalTicketCount, 0)
            validateBookingWindow(performanceDate, bookingCloseAt)

            return Schedule(
                scheduleId = null,
                performanceDate = performanceDate,
                bookingCloseAt = bookingCloseAt,
                totalTicketCount = totalTicketCount,
                soldTicketCount = 0,
                scheduleNumber = scheduleNumber,
                linkedPerformanceId = Performance.Id.from(performanceId)
            )
        }

        @JvmStatic
        fun rehydrate(
            id: Long?,
            performanceDate: LocalDateTime,
            bookingCloseAt: LocalDateTime,
            totalTicketCount: Int,
            soldTicketCount: Int,
            scheduleNumber: ScheduleNumber,
            performanceId: Long,
        ): Schedule {
            validateTicketCounts(totalTicketCount, soldTicketCount)
            validateBookingWindow(performanceDate, bookingCloseAt)

            return Schedule(
                scheduleId = Id.fromNullable(id),
                performanceDate = performanceDate,
                bookingCloseAt = bookingCloseAt,
                totalTicketCount = totalTicketCount,
                soldTicketCount = soldTicketCount,
                scheduleNumber = scheduleNumber,
                linkedPerformanceId = Performance.Id.from(performanceId)
            )
        }

        private fun validateTicketCounts(totalTicketCount: Int, soldTicketCount: Int) {
            if (totalTicketCount < 0 || soldTicketCount < 0 || soldTicketCount > totalTicketCount) {
                throw BadRequestException(ScheduleErrorCode.INVALID_DATA_FORMAT)
            }
        }

        private fun validateBookingWindow(performanceDate: LocalDateTime, bookingCloseAt: LocalDateTime) {
            if (bookingCloseAt.isBefore(performanceDate)) {
                throw BadRequestException(ScheduleErrorCode.INVALID_DATA_FORMAT)
            }
        }
    }
}
