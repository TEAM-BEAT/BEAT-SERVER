package com.beat.apis.performance.api.request

import com.beat.apis.schedule.api.type.ScheduleNumberType
import java.time.LocalDateTime
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class ScheduleRequest(
    @field:NotNull val performanceDate: LocalDateTime?,
    @field:NotNull @field:Positive val totalTicketCount: Int?,
    @field:NotNull val scheduleNumber: ScheduleNumberType?,
)
