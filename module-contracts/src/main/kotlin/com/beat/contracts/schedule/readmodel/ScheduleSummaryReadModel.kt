package com.beat.contracts.schedule.readmodel

import com.beat.contracts.common.ReadModel
import java.time.LocalDateTime

@ReadModel
data class ScheduleSummaryReadModel(
    val scheduleId: Long,
    val performanceDate: LocalDateTime,
    val totalTicketCount: Int,
    val soldTicketCount: Int,
    val scheduleNumber: String,
)
