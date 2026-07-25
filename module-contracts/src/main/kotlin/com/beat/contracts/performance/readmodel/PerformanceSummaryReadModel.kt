package com.beat.contracts.performance.readmodel

import com.beat.contracts.common.ReadModel
import java.time.LocalDate

@ReadModel
data class PerformanceSummaryReadModel(
    val performanceId: Long,
    val userId: Long,
    val performanceTitle: String,
    val genre: String,
    val ticketPrice: Int,
    val bankName: String?,
    val accountNumber: String?,
    val accountHolder: String?,
    val posterImage: String,
    val performanceTeamName: String,
    val performanceVenue: String,
    val performanceContact: String,
    val totalScheduleCount: Int,
    val periodStartDate: LocalDate,
    val periodEndDate: LocalDate,
)
