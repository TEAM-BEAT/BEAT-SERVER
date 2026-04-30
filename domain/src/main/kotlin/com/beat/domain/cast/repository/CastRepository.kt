package com.beat.domain.cast.repository

import com.beat.domain.cast.domain.Cast
import java.util.*

@JvmSuppressWildcards
interface CastRepository {
    fun findById(castId: Long?): Optional<Cast>

    fun findAllByPerformanceId(performanceId: Long?): List<Cast>

    fun findIdsByPerformanceId(performanceId: Long?): List<Long>

    fun save(cast: Cast): Cast

    fun saveAll(casts: List<Cast>): List<Cast>

    fun delete(cast: Cast)

    fun deleteByPerformanceId(performanceId: Long?)
}
