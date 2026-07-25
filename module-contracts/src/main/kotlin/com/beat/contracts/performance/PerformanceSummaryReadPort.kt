package com.beat.contracts.performance

import com.beat.contracts.performance.readmodel.PerformanceSummaryReadModel
import java.util.Optional

interface PerformanceSummaryReadPort {
    fun findById(id: Long): Optional<PerformanceSummaryReadModel>

    fun findAllByIds(ids: Collection<Long>): List<PerformanceSummaryReadModel>

    fun findAll(): List<PerformanceSummaryReadModel>

    fun findByGenre(genre: String): List<PerformanceSummaryReadModel>
}
