package com.beat.apis.performance.api.response

import com.beat.apis.schedule.api.type.ScheduleNumberType
import com.beat.application.frontoffice.performance.maker.ScheduleResult
import java.time.LocalDateTime

@ConsistentCopyVisibility
data class ScheduleResponse private constructor(
    val scheduleId: Long?,
    val performanceDate: LocalDateTime?,
    val totalTicketCount: Int,
    val dueDate: Int,
    val scheduleNumber: ScheduleNumberType?,
) {
    companion object {
        fun from(result: ScheduleResult): ScheduleResponse = ScheduleResponse(
            scheduleId = result.id,
            performanceDate = result.performanceDate,
            totalTicketCount = result.totalTicketCount,
            dueDate = result.dueDate,
            scheduleNumber = result.scheduleNumber?.let(ScheduleNumberType::valueOf),
        )
    }
}
