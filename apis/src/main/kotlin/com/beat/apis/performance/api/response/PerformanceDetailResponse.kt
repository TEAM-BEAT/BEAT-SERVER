package com.beat.apis.performance.api.response

import com.beat.global.support.jackson.CdnImageUrl

data class PerformanceDetailResponse(
    val performanceId: Long?,
    val performanceTitle: String?,
    val performancePeriod: String?,
    val scheduleList: List<PerformanceDetailScheduleResponse>,
    val ticketPrice: Int,
    val genre: String?,
    @field:CdnImageUrl val posterImage: String?,
    val runningTime: Int,
    val performanceVenue: String?,
    val roadAddressName: String?,
    val placeDetailAddress: String?,
    val latitude: String?,
    val longitude: String?,
    val performanceDescription: String?,
    val performanceAttentionNote: String?,
    val performanceContact: String?,
    val performanceTeamName: String?,
    val castList: List<PerformanceDetailCastResponse>,
    val staffList: List<PerformanceDetailStaffResponse>,
    val minDueDate: Int,
    val performanceImageList: List<PerformanceDetailImageResponse>,
)
