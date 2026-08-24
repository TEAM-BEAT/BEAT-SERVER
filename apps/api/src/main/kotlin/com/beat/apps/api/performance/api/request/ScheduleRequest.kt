package com.beat.apps.api.performance.api.request

import com.beat.apps.api.schedule.api.type.ScheduleNumberType
import java.time.LocalDateTime

data class ScheduleRequest(
    val performanceDate: LocalDateTime,
    val totalTicketCount: Int,
    val scheduleNumber: ScheduleNumberType,
)
