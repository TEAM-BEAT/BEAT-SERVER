package com.beat.domain.performance.repository

import com.beat.domain.performance.model.Performance
import java.util.*

@JvmSuppressWildcards
interface PerformanceRepository {
    fun findById(id: Long?): Optional<Performance>

    fun lockById(id: Long?): Optional<Performance>

    fun save(performance: Performance): Performance

    fun deleteById(id: Long?)
}
