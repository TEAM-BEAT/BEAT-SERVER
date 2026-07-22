package com.beat.apis.performance.api.response

import com.beat.apis.schedule.api.type.ScheduleNumberType
import java.time.LocalDateTime

data class ScheduleModifyResponse(
    val scheduleId: Long?,
    val performanceDate: LocalDateTime?,
    val totalTicketCount: Int,
    val dueDate: Int,
    val scheduleNumber: ScheduleNumberType?,
)
