package com.beat.apis.performance.api.request

import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class ScheduleModifyRequest(
    val scheduleId: Long?,
    @field:NotNull val performanceDate: LocalDateTime?,
    @field:NotNull val totalTicketCount: Int?,
)
