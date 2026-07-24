package com.beat.apis.performance.api.response

import com.beat.apis.performance.api.type.BankNameType
import com.beat.apis.performance.api.type.GenreType
import com.beat.apis.performance.application.result.PerformanceMutationResult
import com.beat.global.support.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class PerformanceResponse private constructor(
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
    val scheduleList: List<ScheduleResponse>,
    val castList: List<CastResponse>,
    val staffList: List<StaffResponse>,
    val performanceImageList: List<PerformanceImageResponse>,
) {
    companion object {
        fun from(result: PerformanceMutationResult): PerformanceResponse = PerformanceResponse(
            userId = result.userId,
            performanceId = result.performanceId,
            performanceTitle = result.performanceTitle,
            genre = result.genre?.let(GenreType::valueOf),
            runningTime = result.runningTime,
            performanceDescription = result.performanceDescription,
            performanceAttentionNote = result.performanceAttentionNote,
            bankName = result.bankName?.let(BankNameType::valueOf),
            accountNumber = result.accountNumber,
            accountHolder = result.accountHolder,
            posterImage = result.posterImage,
            performanceTeamName = result.performanceTeamName,
            performanceVenue = result.performanceVenue,
            roadAddressName = result.roadAddressName,
            placeDetailAddress = result.placeDetailAddress,
            latitude = result.latitude,
            longitude = result.longitude,
            performanceContact = result.performanceContact,
            performancePeriod = result.performancePeriod,
            ticketPrice = result.ticketPrice,
            totalScheduleCount = result.totalScheduleCount,
            scheduleList = result.schedules.map(ScheduleResponse::from),
            castList = result.casts.map(CastResponse::from),
            staffList = result.staffs.map(StaffResponse::from),
            performanceImageList = result.images.map(PerformanceImageResponse::from),
        )
    }
}
