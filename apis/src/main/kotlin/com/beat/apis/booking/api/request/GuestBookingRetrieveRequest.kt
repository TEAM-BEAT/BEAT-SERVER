package com.beat.apis.booking.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class GuestBookingRetrieveRequest(
    @field:NotBlank @field:Pattern(regexp = "^[a-zA-Z가-힣]+$") val bookerName: String?,
    @field:NotBlank @field:Pattern(regexp = "^\\d{6}$") val birthDate: String?,
    @field:NotBlank @field:Pattern(regexp = "^\\d{3}-\\d{4}-\\d{4}$") val bookerPhoneNumber: String?,
    @field:NotBlank @field:Pattern(regexp = "^\\d{4}$") val password: String?,
)
