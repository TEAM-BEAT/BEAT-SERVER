package com.beat.apis.schedule.application

import com.beat.domain.schedule.model.Schedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal fun calculateDueDate(today: LocalDate, schedule: Schedule): Int =
    calculateDueDate(today, schedule.getPerformanceDate())

internal fun calculateDueDate(today: LocalDate, performanceDate: LocalDateTime): Int =
    ChronoUnit.DAYS.between(today, performanceDate.toLocalDate()).toInt()
