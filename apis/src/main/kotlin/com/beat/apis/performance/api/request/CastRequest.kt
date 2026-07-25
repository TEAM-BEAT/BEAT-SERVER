package com.beat.apis.performance.api.request

import jakarta.validation.constraints.NotNull

data class CastRequest(
    @field:NotNull val castName: String?,
    @field:NotNull val castRole: String?,
    @field:NotNull val castPhoto: String?,
)
