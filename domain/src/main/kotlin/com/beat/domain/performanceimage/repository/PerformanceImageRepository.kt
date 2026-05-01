package com.beat.domain.performanceimage.repository

import com.beat.domain.performanceimage.domain.PerformanceImage
import java.util.*

@JvmSuppressWildcards
interface PerformanceImageRepository {
    fun findById(id: Long?): Optional<PerformanceImage>

    fun findAllByPerformanceId(performanceId: Long?): List<PerformanceImage>

    fun findIdsByPerformanceId(performanceId: Long?): List<Long>

    fun save(performanceImage: PerformanceImage): PerformanceImage

    fun saveAll(performanceImages: List<PerformanceImage>): List<PerformanceImage>

    fun delete(performanceImage: PerformanceImage)

    fun deleteByPerformanceId(performanceId: Long?)
}
