package com.beat.apis.performance.api.response

import com.beat.apis.performance.api.type.BankNameType
import com.beat.apis.performance.api.type.GenreType
import com.beat.application.frontoffice.performance.maker.PerformanceMutationResult
import com.beat.apis.web.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class PerformanceModifyResponse private constructor(
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
    val scheduleModifyResponses: List<ScheduleModifyResponse>,
    val castModifyResponses: List<CastModifyResponse>,
    val staffModifyResponses: List<StaffModifyResponse>,
    val performanceImageModifyResponses: List<PerformanceImageModifyResponse>,
) {
    companion object {
        fun from(result: PerformanceMutationResult): PerformanceModifyResponse = PerformanceModifyResponse(
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
            scheduleModifyResponses = result.schedules.map(ScheduleModifyResponse::from),
            castModifyResponses = result.casts.map(CastModifyResponse::from),
            staffModifyResponses = result.staffs.map(StaffModifyResponse::from),
            performanceImageModifyResponses = result.images.map(PerformanceImageModifyResponse::from),
        )
    }
}
