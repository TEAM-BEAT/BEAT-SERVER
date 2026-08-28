package com.beat.apps.api.fixture

import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.user.model.Users
import java.time.LocalDateTime

internal fun usersFixture(): Users = Users.create()

internal fun performanceFixture(
    userId: Long,
    ticketPrice: Int,
    performancePeriod: PerformancePeriod,
    totalScheduleCount: Int,
    performanceTitle: String = "concurrency performance",
    genre: Genre = Genre.BAND,
    runningTime: RunningTime = RunningTime.of(120),
    performanceDescription: String = "description",
    performanceAttentionNote: String = "attention",
    paymentAccount: PaymentAccount? = null,
    posterImage: String = "poster.jpg",
    performanceTeamName: String = "team",
    performanceVenue: String = "venue",
    roadAddressName: String = "road",
    placeDetailAddress: String = "detail",
    latitude: String = "37.0",
    longitude: String = "127.0",
    performanceContact: String = "010-0000-0000",
): Performance =
    Performance.create(
        performanceTitle = performanceTitle,
        genre = genre,
        runningTime = runningTime,
        performanceDescription = performanceDescription,
        performanceAttentionNote = performanceAttentionNote,
        paymentAccount = paymentAccount,
        posterImage = posterImage,
        performanceTeamName = performanceTeamName,
        performanceVenue = performanceVenue,
        roadAddressName = roadAddressName,
        placeDetailAddress = placeDetailAddress,
        latitude = latitude,
        longitude = longitude,
        performanceContact = performanceContact,
        performancePeriod = performancePeriod,
        ticketPrice = TicketPrice.of(ticketPrice),
        totalScheduleCount = totalScheduleCount,
        userId = userId,
    )

internal fun scheduleFixture(
    performanceId: Long,
    performanceDate: LocalDateTime,
    bookingCloseAt: LocalDateTime,
    totalTicketCount: Int,
    scheduleNumber: ScheduleNumber,
): Schedule =
    Schedule.create(
        performanceDate = performanceDate,
        bookingCloseAt = bookingCloseAt,
        totalTicketCount = totalTicketCount,
        scheduleNumber = scheduleNumber,
        performanceId = performanceId,
    )
