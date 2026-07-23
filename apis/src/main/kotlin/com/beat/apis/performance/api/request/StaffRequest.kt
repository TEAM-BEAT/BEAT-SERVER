package com.beat.apis.performance.api.request

import jakarta.validation.constraints.NotNull

data class StaffRequest(
    @field:NotNull val staffName: String?,
    @field:NotNull val staffRole: String?,
    @field:NotNull val staffPhoto: String?,
)
