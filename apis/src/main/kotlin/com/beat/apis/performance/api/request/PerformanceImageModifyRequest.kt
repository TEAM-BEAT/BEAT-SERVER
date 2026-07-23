package com.beat.apis.performance.api.request

import jakarta.validation.constraints.NotNull

data class PerformanceImageModifyRequest(
    val performanceImageId: Long?,
    @field:NotNull val performanceImage: String?,
)
