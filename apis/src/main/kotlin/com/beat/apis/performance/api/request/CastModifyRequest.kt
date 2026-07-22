package com.beat.apis.performance.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class CastModifyRequest(
    @field:Positive val castId: Long?,
    @field:NotBlank val castName: String?,
    @field:NotBlank val castRole: String?,
    @field:NotBlank val castPhoto: String?,
)
