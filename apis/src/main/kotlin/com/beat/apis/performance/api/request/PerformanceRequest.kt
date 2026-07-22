package com.beat.apis.performance.api.request

import com.beat.apis.performance.api.type.BankNameType
import com.beat.apis.performance.api.type.GenreType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class PerformanceRequest(
    @field:NotBlank val performanceTitle: String?,
    @field:NotNull val genre: GenreType?,
    @field:NotNull @field:Positive val runningTime: Int?,
    @field:NotBlank @field:Size(max = 1500, message = "공연 소개는 1500자를 초과할 수 없습니다.")
    val performanceDescription: String?,
    @field:NotBlank @field:Size(max = 1500, message = "공연 유의사항은 1500자를 초과할 수 없습니다.")
    val performanceAttentionNote: String?,
    val bankName: BankNameType?,
    val accountNumber: String?,
    val accountHolder: String?,
    @field:NotBlank val posterImage: String?,
    @field:NotBlank val performanceTeamName: String?,
    @field:NotBlank val performanceVenue: String?,
    @field:NotBlank val roadAddressName: String?,
    @field:NotBlank val placeDetailAddress: String?,
    @field:NotBlank val latitude: String?,
    @field:NotBlank val longitude: String?,
    @field:NotBlank val performanceContact: String?,
    @field:NotBlank val performancePeriod: String?,
    @field:NotNull @field:PositiveOrZero val ticketPrice: Int?,
    @field:NotNull @field:Positive val totalScheduleCount: Int?,
    @field:NotNull @field:Size(min = 1) @field:Valid
    val scheduleList: List<@NotNull @Valid ScheduleRequest>?,
    @field:NotNull @field:Valid val castList: List<@NotNull @Valid CastRequest>?,
    @field:NotNull @field:Valid val staffList: List<@NotNull @Valid StaffRequest>?,
    @field:NotNull @field:Valid
    val performanceImageList: List<@NotNull @Valid PerformanceImageRequest>?,
)
