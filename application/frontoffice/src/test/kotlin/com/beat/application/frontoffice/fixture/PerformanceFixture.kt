package com.beat.application.frontoffice.fixture

import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import java.time.LocalDate
import java.time.LocalDateTime

fun frontofficePerformanceFixture(
    id: Long = 20L,
    performanceTitle: String = "title",
    userId: Long = 1L,
    ticketPrice: Int = 10_000,
    totalScheduleCount: Int = 1,
    paymentAccount: PaymentAccount? = null,
): Performance = Performance.rehydrate(
    id = id,
    performanceTitle = performanceTitle,
    genre = Genre.BAND,
    runningTime = RunningTime.of(120),
    performanceDescription = "description",
    performanceAttentionNote = "attention",
    paymentAccount = paymentAccount,
    posterImage = "poster",
    performanceTeamName = "team",
    performanceVenue = "venue",
    roadAddressName = "road",
    placeDetailAddress = "detail",
    latitude = "37.0",
    longitude = "127.0",
    performanceContact = "010-0000-0000",
    performancePeriod = PerformancePeriod.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)),
    ticketPrice = TicketPrice.of(ticketPrice),
    totalScheduleCount = totalScheduleCount,
    userId = userId,
)

fun frontofficeScheduleFixture(
    id: Long = 1L,
    performanceId: Long = 20L,
    performanceDate: LocalDateTime = LocalDateTime.of(2026, 2, 1, 18, 0),
    totalTicketCount: Int = 10,
    allocatedTicketCount: Int = 0,
    scheduleNumber: ScheduleNumber = ScheduleNumber.FIRST,
): Schedule = Schedule.rehydrate(
    id = id,
    performanceDate = performanceDate,
    bookingCloseAt = performanceDate.plusHours(2),
    totalTicketCount = totalTicketCount,
    allocatedTicketCount = allocatedTicketCount,
    scheduleNumber = scheduleNumber,
    performanceId = performanceId,
)
