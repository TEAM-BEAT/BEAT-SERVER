package com.beat.application.frontoffice.performance.maker.query

import java.time.LocalDateTime

data class PerformanceEditFormReadModel(
    val performanceId: Long,
    val userId: Long,
    val performanceTitle: String,
    val genre: String,
    val runningTime: Int,
    val performanceDescription: String,
    val performanceAttentionNote: String,
    val bankName: String?,
    val accountNumber: String?,
    val accountHolder: String?,
    val posterImage: String,
    val performanceTeamName: String,
    val performanceVenue: String,
    val roadAddressName: String,
    val placeDetailAddress: String,
    val latitude: String,
    val longitude: String,
    val performanceContact: String,
    val performancePeriod: String,
    val ticketPrice: Int,
    val totalScheduleCount: Int,
    val hasActiveBooking: Boolean,
    val schedules: List<PerformanceEditScheduleReadModel>,
    val casts: List<PerformanceEditCastReadModel>,
    val staffs: List<PerformanceEditStaffReadModel>,
    val images: List<PerformanceEditImageReadModel>,
)

data class PerformanceEditScheduleReadModel(
    val id: Long,
    val performanceDate: LocalDateTime,
    val totalTicketCount: Int,
    val scheduleNumber: String,
)

data class PerformanceEditCastReadModel(
    val id: Long,
    val name: String,
    val role: String,
    val photo: String,
)

data class PerformanceEditStaffReadModel(
    val id: Long,
    val name: String,
    val role: String,
    val photo: String,
)

data class PerformanceEditImageReadModel(
    val id: Long,
    val url: String,
)
