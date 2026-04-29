package com.beat.domain.schedule.domain

import com.beat.domain.performance.domain.PerformanceId
import com.beat.domain.schedule.exception.ScheduleErrorCode
import com.beat.global.common.exception.ConflictException
import java.time.LocalDateTime
import kotlin.ConsistentCopyVisibility

@ConsistentCopyVisibility
data class Schedule private constructor(
    private val scheduleId: Id?,
    private val performanceDate: LocalDateTime,
    private val totalTicketCount: Int,
    private val soldTicketCount: Int,
    private val isBooking: Boolean,
    private val scheduleNumber: ScheduleNumber,
    private val linkedPerformanceId: PerformanceId,
) {
    fun getId(): Long? = scheduleId?.value

    fun getPerformanceId(): Long = linkedPerformanceId.value

    fun getPerformanceDate(): LocalDateTime = performanceDate

    fun getTotalTicketCount(): Int = totalTicketCount

    fun getSoldTicketCount(): Int = soldTicketCount

    fun isBooking(): Boolean = isBooking

    fun getScheduleNumber(): ScheduleNumber = scheduleNumber

    fun update(performanceDate: LocalDateTime, totalTicketCount: Int, scheduleNumber: ScheduleNumber): Schedule = copy(
        performanceDate = performanceDate,
        totalTicketCount = totalTicketCount,
        scheduleNumber = scheduleNumber
    )

    fun increaseSoldTicketCount(count: Int): Schedule = copy(soldTicketCount = soldTicketCount + count)

    fun decreaseSoldTicketCount(count: Int): Schedule {
        if (soldTicketCount < count) {
            throw ConflictException(ScheduleErrorCode.EXCESS_TICKET_DELETE)
        }
        return copy(soldTicketCount = soldTicketCount - count)
    }

    fun updateScheduleNumber(scheduleNumber: ScheduleNumber): Schedule = copy(scheduleNumber = scheduleNumber)

    fun updateIsBooking(isBooking: Boolean): Schedule = copy(isBooking = isBooking)

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
            totalTicketCount: Int,
            scheduleNumber: ScheduleNumber,
            performanceId: Long,
        ): Schedule = Schedule(
            scheduleId = null,
            performanceDate = performanceDate,
            totalTicketCount = totalTicketCount,
            soldTicketCount = 0,
            isBooking = true,
            scheduleNumber = scheduleNumber,
            linkedPerformanceId = PerformanceId.from(performanceId)
        )

        @JvmStatic
        fun rehydrate(
            id: Long?,
            performanceDate: LocalDateTime,
            totalTicketCount: Int,
            soldTicketCount: Int,
            isBooking: Boolean,
            scheduleNumber: ScheduleNumber,
            performanceId: Long,
        ): Schedule = Schedule(
            scheduleId = Id.fromNullable(id),
            performanceDate = performanceDate,
            totalTicketCount = totalTicketCount,
            soldTicketCount = soldTicketCount,
            isBooking = isBooking,
            scheduleNumber = scheduleNumber,
            linkedPerformanceId = PerformanceId.from(performanceId)
        )
    }
}
