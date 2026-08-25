package com.beat.domain.performance.repository

import com.beat.domain.performance.model.Performance

interface PerformanceRepository {
    fun findById(id: Long): Performance?

    fun lockById(id: Long): Performance?

    fun save(performance: Performance): Performance

    fun deleteById(id: Long)
}
