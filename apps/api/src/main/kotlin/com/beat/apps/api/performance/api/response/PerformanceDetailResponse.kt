package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.booker.query.PerformanceDetailResult
import com.beat.apps.api.web.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class PerformanceDetailResponse
private constructor(
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
) {
    companion object {
        fun from(result: PerformanceDetailResult): PerformanceDetailResponse =
            PerformanceDetailResponse(
                performanceId = result.performanceId,
                performanceTitle = result.performanceTitle,
                performancePeriod = result.performancePeriod,
                scheduleList = result.schedules.map(PerformanceDetailScheduleResponse::from),
                ticketPrice = result.ticketPrice,
                genre = result.genre,
                posterImage = result.posterImage,
                runningTime = result.runningTime,
                performanceVenue = result.performanceVenue,
                roadAddressName = result.roadAddressName,
                placeDetailAddress = result.placeDetailAddress,
                latitude = result.latitude,
                longitude = result.longitude,
                performanceDescription = result.performanceDescription,
                performanceAttentionNote = result.performanceAttentionNote,
                performanceContact = result.performanceContact,
                performanceTeamName = result.performanceTeamName,
                castList = result.casts.map(PerformanceDetailCastResponse::from),
                staffList = result.staffs.map(PerformanceDetailStaffResponse::from),
                minDueDate = result.minDueDate,
                performanceImageList = result.images.map(PerformanceDetailImageResponse::from),
            )
    }
}
