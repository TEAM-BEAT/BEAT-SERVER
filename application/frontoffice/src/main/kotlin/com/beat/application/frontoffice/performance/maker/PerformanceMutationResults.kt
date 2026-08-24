package com.beat.application.frontoffice.performance.maker

import com.beat.application.frontoffice.performance.CastResult
import com.beat.application.frontoffice.performance.PerformanceImageResult
import com.beat.application.frontoffice.performance.StaffResult
import java.time.LocalDateTime

data class ScheduleResult(
    val id: Long?,
    val performanceDate: LocalDateTime?,
    val totalTicketCount: Int,
    val dueDate: Int,
    val scheduleNumber: String?,
)

data class PerformanceMutationResult(
    val userId: Long?,
    val performanceId: Long?,
    val performanceTitle: String?,
    val genre: String?,
    val runningTime: Int,
    val performanceDescription: String?,
    val performanceAttentionNote: String?,
    val bankName: String?,
    val accountNumber: String?,
    val accountHolder: String?,
    val posterImage: String?,
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
    val schedules: List<ScheduleResult>,
    val casts: List<CastResult>,
    val staffs: List<StaffResult>,
    val images: List<PerformanceImageResult>,
)
