package com.beat.domain.performance.repository

import com.beat.domain.performance.domain.Genre
import com.beat.domain.performance.domain.Performance
import java.util.*

@JvmSuppressWildcards
interface PerformanceRepository {
    fun findById(id: Long?): Optional<Performance>

    fun findAll(): List<Performance>

    fun findAllById(ids: Collection<Long>): List<Performance>

    fun save(performance: Performance): Performance

    fun findByGenre(genre: Genre): List<Performance>

    fun findByUserId(userId: Long?): List<Performance>

    fun deleteById(id: Long?)
}
