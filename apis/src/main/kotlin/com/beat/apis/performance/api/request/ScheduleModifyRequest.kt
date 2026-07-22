package com.beat.apis.performance.api.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

data class ScheduleModifyRequest(
    @field:Positive val scheduleId: Long?,
    @field:NotNull val performanceDate: LocalDateTime?,
    @field:NotNull @field:Positive val totalTicketCount: Int?,
)
