package com.beat.apps.api.performance.api.request

import com.beat.apps.api.performance.api.type.BankNameType
import com.beat.apps.api.performance.api.type.GenreType
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

data class PerformanceRequest(
    val performanceTitle: String,
    val genre: GenreType,
    val runningTime: Int,
    @field:Size(max = 1500, message = "공연 소개는 1500자를 초과할 수 없습니다.")
    val performanceDescription: String,
    @field:Size(max = 1500, message = "공연 유의사항은 1500자를 초과할 수 없습니다.")
    val performanceAttentionNote: String,
    val bankName: BankNameType?,
    val accountNumber: String?,
    val accountHolder: String?,
    val posterImage: String,
    val performanceTeamName: String,
    val performanceVenue: String,
    val roadAddressName: String,
    val placeDetailAddress: String,
    val latitude: String,
    val longitude: String,
    val performanceContact: String,
    val performancePeriod: String?,
    val ticketPrice: Int,
    val totalScheduleCount: Int?,
    @field:Valid val scheduleList: List<@Valid ScheduleRequest>,
    @field:Valid val castList: List<@Valid CastRequest>,
    @field:Valid val staffList: List<@Valid StaffRequest>,
    @field:Valid val performanceImageList: List<@Valid PerformanceImageRequest>,
)
