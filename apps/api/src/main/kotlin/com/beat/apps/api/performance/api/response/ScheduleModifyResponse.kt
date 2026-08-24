package com.beat.apps.api.performance.api.response

import com.beat.apps.api.schedule.api.type.ScheduleNumberType
import com.beat.application.frontoffice.performance.maker.ScheduleResult
import java.time.LocalDateTime

@ConsistentCopyVisibility
data class ScheduleModifyResponse private constructor(
    val scheduleId: Long?,
    val performanceDate: LocalDateTime?,
    val totalTicketCount: Int,
    val dueDate: Int,
    val scheduleNumber: ScheduleNumberType?,
) {
    companion object {
        fun from(result: ScheduleResult): ScheduleModifyResponse = ScheduleModifyResponse(
            scheduleId = result.id,
            performanceDate = result.performanceDate,
            totalTicketCount = result.totalTicketCount,
            dueDate = result.dueDate,
            scheduleNumber = result.scheduleNumber?.let(ScheduleNumberType::valueOf),
        )
    }
}
