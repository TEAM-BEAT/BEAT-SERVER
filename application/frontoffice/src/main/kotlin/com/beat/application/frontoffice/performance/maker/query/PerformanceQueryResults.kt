package com.beat.application.frontoffice.performance.maker.query

import java.time.LocalDateTime

data class CastResult(val id: Long?, val name: String?, val role: String?, val photo: String?)

data class StaffResult(val id: Long?, val name: String?, val role: String?, val photo: String?)

data class PerformanceImageResult(val id: Long?, val image: String?)

data class ScheduleResult(
    val id: Long?,
    val performanceDate: LocalDateTime?,
    val totalTicketCount: Int,
    val dueDate: Int,
    val scheduleNumber: String?,
)

data class PerformanceEditResult(
    val performance: PerformanceEditPerformanceResult,
    val isBookerExist: Boolean,
)

data class PerformanceEditPerformanceResult(
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

data class MakerPerformanceListResult(
    val userId: Long?,
    val performances: List<MakerPerformanceResult>,
)

data class MakerPerformanceResult(
    val performanceId: Long?,
    val genre: String?,
    val performanceTitle: String?,
    val posterImage: String?,
    val performancePeriod: String?,
    val minDueDate: Int,
)
