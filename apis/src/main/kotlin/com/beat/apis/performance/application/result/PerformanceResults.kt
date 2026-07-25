package com.beat.apis.performance.application.result

import java.time.LocalDateTime

data class ScheduleResult(
    val id: Long?,
    val performanceDate: LocalDateTime?,
    val totalTicketCount: Int,
    val dueDate: Int,
    val scheduleNumber: String?,
)

data class CastResult(val id: Long?, val name: String?, val role: String?, val photo: String?)
data class StaffResult(val id: Long?, val name: String?, val role: String?, val photo: String?)
data class PerformanceImageResult(val id: Long?, val image: String?)

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

data class PerformanceEditResult(
    val performance: PerformanceMutationResult,
    val isBookerExist: Boolean,
)

data class PerformanceDetailResult(
    val performanceId: Long?,
    val performanceTitle: String?,
    val performancePeriod: String?,
    val schedules: List<PerformanceDetailScheduleResult>,
    val ticketPrice: Int,
    val genre: String?,
    val posterImage: String?,
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
    val casts: List<CastResult>,
    val staffs: List<StaffResult>,
    val minDueDate: Int,
    val images: List<PerformanceImageResult>,
)

data class PerformanceDetailScheduleResult(
    val scheduleId: Long?,
    val performanceDate: LocalDateTime?,
    val scheduleNumber: String?,
    val dueDate: Int,
    val isBooking: Boolean,
)

data class BookingPerformanceDetailResult(
    val performanceId: Long?,
    val performanceTitle: String?,
    val performancePeriod: String?,
    val schedules: List<BookingPerformanceScheduleResult>,
    val ticketPrice: Int,
    val genre: String?,
    val posterImage: String?,
    val performanceVenue: String?,
    val performanceTeamName: String?,
    val bankName: String?,
    val accountNumber: String?,
    val accountHolder: String?,
)

data class BookingPerformanceScheduleResult(
    val scheduleId: Long?,
    val performanceDate: LocalDateTime?,
    val scheduleNumber: String?,
    val availableTicketCount: Int,
    val isBooking: Boolean,
    val dueDate: Int,
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
