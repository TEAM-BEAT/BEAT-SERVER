package com.beat.apis.performance.api.request

import java.time.LocalDateTime

data class ScheduleModifyRequest(
    val scheduleId: Long?,
    val performanceDate: LocalDateTime,
    val totalTicketCount: Int,
)
