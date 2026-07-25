package com.beat.apis.performance.api.request

import jakarta.validation.constraints.NotNull

data class PerformanceImageRequest(
    @field:NotNull val performanceImage: String?,
)
