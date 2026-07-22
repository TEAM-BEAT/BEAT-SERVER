package com.beat.apis.performance.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class StaffModifyRequest(
    @field:Positive val staffId: Long?,
    @field:NotBlank val staffName: String?,
    @field:NotBlank val staffRole: String?,
    @field:NotBlank val staffPhoto: String?,
)
