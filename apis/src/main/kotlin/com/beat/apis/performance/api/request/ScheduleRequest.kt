package com.beat.apis.performance.api.request

import com.beat.apis.schedule.api.type.ScheduleNumberType
import java.time.LocalDateTime

data class ScheduleRequest(
    val performanceDate: LocalDateTime,
    val totalTicketCount: Int,
    val scheduleNumber: ScheduleNumberType,
)
