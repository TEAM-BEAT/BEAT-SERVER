package com.beat.apis.performance.api.request

import jakarta.validation.constraints.NotBlank

data class CastRequest(
    @field:NotBlank val castName: String?,
    @field:NotBlank val castRole: String?,
    @field:NotBlank val castPhoto: String?,
)
