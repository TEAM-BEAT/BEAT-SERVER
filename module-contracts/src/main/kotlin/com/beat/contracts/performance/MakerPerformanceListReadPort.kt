package com.beat.contracts.performance

import com.beat.contracts.performance.readmodel.MakerPerformanceListItemReadModel

fun interface MakerPerformanceListReadPort {
    fun findByUserId(userId: Long): List<MakerPerformanceListItemReadModel>
}
