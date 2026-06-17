package com.beat.contracts.schedule.readmodel

import com.beat.contracts.common.ReadModel
import java.time.LocalDateTime

@ReadModel
data class MinPerformanceDateReadModel(
    val performanceId: Long,
    val performanceDate: LocalDateTime,
)
