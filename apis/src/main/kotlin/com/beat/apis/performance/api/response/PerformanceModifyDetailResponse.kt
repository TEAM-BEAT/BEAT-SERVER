package com.beat.apis.performance.api.response

import com.beat.apis.performance.api.type.BankNameType
import com.beat.apis.performance.api.type.GenreType
import com.beat.global.support.jackson.CdnImageUrl

data class PerformanceModifyDetailResponse(
    val userId: Long?,
    val performanceId: Long?,
    val performanceTitle: String?,
    val genre: GenreType?,
    val runningTime: Int,
    val performanceDescription: String?,
    val performanceAttentionNote: String?,
    val bankName: BankNameType?,
    val accountNumber: String?,
    val accountHolder: String?,
    @field:CdnImageUrl val posterImage: String?,
    val performanceTeamName: String?,
    val performanceVenue: String?,
    val roadAddressName: String?,
    val placeDetailAddress: String?,
    val latitude: String?,
    val longitude: String?,
    val performanceContact: String?,
    val performancePeriod: String?,
    val ticketPrice: Int,
    val totalScheduleCount: Int,
    val isBookerExist: Boolean,
    val scheduleList: List<ScheduleResponse>,
    val castList: List<CastResponse>,
    val staffList: List<StaffResponse>,
    val performanceImageList: List<PerformanceImageResponse>,
)
