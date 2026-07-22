package com.beat.apis.performance.api.request

import jakarta.validation.constraints.NotBlank

data class StaffRequest(
    @field:NotBlank val staffName: String?,
    @field:NotBlank val staffRole: String?,
    @field:NotBlank val staffPhoto: String?,
)
