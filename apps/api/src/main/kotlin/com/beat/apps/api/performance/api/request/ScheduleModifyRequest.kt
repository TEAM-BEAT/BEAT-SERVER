package com.beat.apps.api.performance.api.request

import java.time.LocalDateTime

data class ScheduleModifyRequest(
    val scheduleId: Long?,
    val performanceDate: LocalDateTime,
    val totalTicketCount: Int,
)
