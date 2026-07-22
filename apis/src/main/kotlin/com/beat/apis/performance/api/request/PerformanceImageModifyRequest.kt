package com.beat.apis.performance.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class PerformanceImageModifyRequest(
    @field:Positive val performanceImageId: Long?,
    @field:NotBlank val performanceImage: String?,
)
