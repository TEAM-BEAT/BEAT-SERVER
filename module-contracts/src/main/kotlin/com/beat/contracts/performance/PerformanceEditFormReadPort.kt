package com.beat.contracts.performance

import com.beat.contracts.performance.readmodel.PerformanceEditFormReadModel
import java.util.Optional

fun interface PerformanceEditFormReadPort {

    fun findByPerformanceId(performanceId: Long): Optional<PerformanceEditFormReadModel>
}
