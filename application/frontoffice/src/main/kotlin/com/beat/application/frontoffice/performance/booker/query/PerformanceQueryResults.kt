package com.beat.application.frontoffice.performance.booker.query

import com.beat.application.frontoffice.performance.CastResult
import com.beat.application.frontoffice.performance.PerformanceImageResult
import com.beat.application.frontoffice.performance.StaffResult
import java.time.LocalDateTime

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
