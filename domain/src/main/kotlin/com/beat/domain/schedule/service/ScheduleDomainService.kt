package com.beat.domain.schedule.service

import com.beat.domain.schedule.domain.Schedule
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ScheduleDomainService {
    fun calculateDueDate(today: LocalDate, schedule: Schedule): Int =
        ChronoUnit.DAYS.between(today, schedule.getPerformanceDate().toLocalDate()).toInt()

    fun getMinDueDate(today: LocalDate, schedules: List<Schedule>): Int {
        val dueDates = schedules.map { calculateDueDate(today, it) }
        return dueDates.filter { it >= 0 }.minOrNull()
            ?: dueDates.minOrNull()
            ?: Int.MAX_VALUE
    }

    fun getAvailableTicketCount(schedule: Schedule): Int =
        schedule.getTotalTicketCount() - schedule.getSoldTicketCount()

    fun canPurchase(schedule: Schedule, purchaseTicketCount: Int): Boolean =
        getAvailableTicketCount(schedule) >= purchaseTicketCount
}
