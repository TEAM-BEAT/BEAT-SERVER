package com.beat.apis.performance.api.response

import com.beat.apis.performance.api.type.BankNameType
import com.beat.apis.performance.api.type.GenreType
import com.beat.application.frontoffice.performance.maker.query.PerformanceEditResult
import com.beat.apis.web.jackson.CdnImageUrl
import com.fasterxml.jackson.annotation.JsonProperty

@ConsistentCopyVisibility
data class PerformanceModifyDetailResponse private constructor(
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
    @get:JsonProperty("isBookerExist")
    val isBookerExist: Boolean,
    val scheduleList: List<ScheduleResponse>,
    val castList: List<CastResponse>,
    val staffList: List<StaffResponse>,
    val performanceImageList: List<PerformanceImageResponse>,
) {
    companion object {
        fun from(result: PerformanceEditResult): PerformanceModifyDetailResponse {
            val performance = result.performance
            return PerformanceModifyDetailResponse(
                userId = performance.userId,
                performanceId = performance.performanceId,
                performanceTitle = performance.performanceTitle,
                genre = performance.genre?.let(GenreType::valueOf),
                runningTime = performance.runningTime,
                performanceDescription = performance.performanceDescription,
                performanceAttentionNote = performance.performanceAttentionNote,
                bankName = performance.bankName?.let(BankNameType::valueOf),
                accountNumber = performance.accountNumber,
                accountHolder = performance.accountHolder,
                posterImage = performance.posterImage,
                performanceTeamName = performance.performanceTeamName,
                performanceVenue = performance.performanceVenue,
                roadAddressName = performance.roadAddressName,
                placeDetailAddress = performance.placeDetailAddress,
                latitude = performance.latitude,
                longitude = performance.longitude,
                performanceContact = performance.performanceContact,
                performancePeriod = performance.performancePeriod,
                ticketPrice = performance.ticketPrice,
                totalScheduleCount = performance.totalScheduleCount,
                isBookerExist = result.isBookerExist,
                scheduleList = performance.schedules.map(ScheduleResponse::from),
                castList = performance.casts.map(CastResponse::from),
                staffList = performance.staffs.map(StaffResponse::from),
                performanceImageList = performance.images.map(PerformanceImageResponse::from),
            )
        }
    }
}
