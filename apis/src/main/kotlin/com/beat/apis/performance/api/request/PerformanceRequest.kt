package com.beat.apis.performance.api.request

import com.beat.apis.performance.api.type.BankNameType
import com.beat.apis.performance.api.type.GenreType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class PerformanceRequest(
    @field:NotNull val performanceTitle: String?,
    @field:NotNull val genre: GenreType?,
    @field:NotNull val runningTime: Int?,
    @field:NotNull @field:Size(max = 1500, message = "공연 소개는 1500자를 초과할 수 없습니다.")
    val performanceDescription: String?,
    @field:NotNull @field:Size(max = 1500, message = "공연 유의사항은 1500자를 초과할 수 없습니다.")
    val performanceAttentionNote: String?,
    val bankName: BankNameType?,
    val accountNumber: String?,
    val accountHolder: String?,
    @field:NotNull val posterImage: String?,
    @field:NotNull val performanceTeamName: String?,
    @field:NotNull val performanceVenue: String?,
    @field:NotNull val roadAddressName: String?,
    @field:NotNull val placeDetailAddress: String?,
    @field:NotNull val latitude: String?,
    @field:NotNull val longitude: String?,
    @field:NotNull val performanceContact: String?,
    @field:NotNull val performancePeriod: String?,
    @field:NotNull val ticketPrice: Int?,
    @field:NotNull val totalScheduleCount: Int?,
    @field:NotNull @field:Valid val scheduleList: List<@Valid ScheduleRequest>?,
    @field:NotNull @field:Valid val castList: List<@Valid CastRequest>?,
    @field:NotNull @field:Valid val staffList: List<@Valid StaffRequest>?,
    @field:NotNull @field:Valid val performanceImageList: List<@Valid PerformanceImageRequest>?,
)
