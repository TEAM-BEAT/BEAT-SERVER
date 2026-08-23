package com.beat.apis.performance.api.request

import com.beat.apis.performance.api.type.BankNameType
import com.beat.apis.performance.api.type.GenreType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class PerformanceModifyRequest(
    val performanceId: Long,
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
    @field:NotNull val performancePeriod: String?,
    @field:NotNull val totalScheduleCount: Int?,
    val ticketPrice: Int,
    @field:Valid val scheduleModifyRequests: List<@Valid ScheduleModifyRequest>,
    @field:Valid val castModifyRequests: List<@Valid CastModifyRequest>,
    @field:Valid val staffModifyRequests: List<@Valid StaffModifyRequest>,
    @field:Valid val performanceImageModifyRequests: List<@Valid PerformanceImageModifyRequest>,
)
