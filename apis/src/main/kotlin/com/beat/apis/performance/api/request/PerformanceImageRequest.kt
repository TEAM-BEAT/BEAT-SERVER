package com.beat.apis.performance.api.request

import jakarta.validation.constraints.NotBlank

data class PerformanceImageRequest(
    @field:NotBlank val performanceImage: String?,
)
