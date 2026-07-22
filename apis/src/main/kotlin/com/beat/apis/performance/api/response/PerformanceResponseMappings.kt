package com.beat.apis.performance.api.response

import com.beat.apis.performance.api.type.BankNameType
import com.beat.apis.performance.api.type.GenreType
import com.beat.apis.performance.application.result.BookingPerformanceDetailResult
import com.beat.apis.performance.application.result.MakerPerformanceListResult
import com.beat.apis.performance.application.result.PerformanceDetailResult
import com.beat.apis.performance.application.result.PerformanceEditResult
import com.beat.apis.performance.application.result.PerformanceMutationResult
import com.beat.apis.performance.application.result.ScheduleResult
import com.beat.apis.schedule.api.type.ScheduleNumberType

internal fun PerformanceMutationResult.toPerformanceResponse(): PerformanceResponse = PerformanceResponse(
    userId = userId,
    performanceId = performanceId,
    performanceTitle = performanceTitle,
    genre = genre?.let(GenreType::valueOf),
    runningTime = runningTime,
    performanceDescription = performanceDescription,
    performanceAttentionNote = performanceAttentionNote,
    bankName = bankName?.let(BankNameType::valueOf),
    accountNumber = accountNumber,
    accountHolder = accountHolder,
    posterImage = posterImage,
    performanceTeamName = performanceTeamName,
    performanceVenue = performanceVenue,
    roadAddressName = roadAddressName,
    placeDetailAddress = placeDetailAddress,
    latitude = latitude,
    longitude = longitude,
    performanceContact = performanceContact,
    performancePeriod = performancePeriod,
    ticketPrice = ticketPrice,
    totalScheduleCount = totalScheduleCount,
    scheduleList = schedules.map { result -> result.toScheduleResponse() },
    castList = casts.map { result -> CastResponse(result.id, result.name, result.role, result.photo) },
    staffList = staffs.map { result -> StaffResponse(result.id, result.name, result.role, result.photo) },
    performanceImageList = images.map { result -> PerformanceImageResponse(result.id, result.image) },
)

internal fun PerformanceMutationResult.toPerformanceModifyResponse(): PerformanceModifyResponse =
    PerformanceModifyResponse(
        userId = userId,
        performanceId = performanceId,
        performanceTitle = performanceTitle,
        genre = genre?.let(GenreType::valueOf),
        runningTime = runningTime,
        performanceDescription = performanceDescription,
        performanceAttentionNote = performanceAttentionNote,
        bankName = bankName?.let(BankNameType::valueOf),
        accountNumber = accountNumber,
        accountHolder = accountHolder,
        posterImage = posterImage,
        performanceTeamName = performanceTeamName,
        performanceVenue = performanceVenue,
        roadAddressName = roadAddressName,
        placeDetailAddress = placeDetailAddress,
        latitude = latitude,
        longitude = longitude,
        performanceContact = performanceContact,
        performancePeriod = performancePeriod,
        ticketPrice = ticketPrice,
        totalScheduleCount = totalScheduleCount,
        scheduleModifyResponses = schedules.map { result ->
            ScheduleModifyResponse(
                scheduleId = result.id,
                performanceDate = result.performanceDate,
                totalTicketCount = result.totalTicketCount,
                dueDate = result.dueDate,
                scheduleNumber = result.scheduleNumber?.let(ScheduleNumberType::valueOf),
            )
        },
        castModifyResponses = casts.map { result ->
            CastModifyResponse(result.id, result.name, result.role, result.photo)
        },
        staffModifyResponses = staffs.map { result ->
            StaffModifyResponse(result.id, result.name, result.role, result.photo)
        },
        performanceImageModifyResponses = images.map { result ->
            PerformanceImageModifyResponse(result.id, result.image)
        },
    )

internal fun PerformanceEditResult.toPerformanceModifyDetailResponse(): PerformanceModifyDetailResponse {
    val result = performance
    return PerformanceModifyDetailResponse(
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
        isBookerExist = isBookerExist,
        scheduleList = result.schedules.map { schedule -> schedule.toScheduleResponse() },
        castList = result.casts.map { cast -> CastResponse(cast.id, cast.name, cast.role, cast.photo) },
        staffList = result.staffs.map { staff -> StaffResponse(staff.id, staff.name, staff.role, staff.photo) },
        performanceImageList = result.images.map { image -> PerformanceImageResponse(image.id, image.image) },
    )
}

internal fun PerformanceDetailResult.toPerformanceDetailResponse(): PerformanceDetailResponse = PerformanceDetailResponse(
    performanceId = performanceId,
    performanceTitle = performanceTitle,
    performancePeriod = performancePeriod,
    scheduleList = schedules.map { result ->
        PerformanceDetailScheduleResponse(
            result.scheduleId, result.performanceDate, result.scheduleNumber, result.dueDate, result.isBooking,
        )
    },
    ticketPrice = ticketPrice,
    genre = genre,
    posterImage = posterImage,
    runningTime = runningTime,
    performanceVenue = performanceVenue,
    roadAddressName = roadAddressName,
    placeDetailAddress = placeDetailAddress,
    latitude = latitude,
    longitude = longitude,
    performanceDescription = performanceDescription,
    performanceAttentionNote = performanceAttentionNote,
    performanceContact = performanceContact,
    performanceTeamName = performanceTeamName,
    castList = casts.map { result -> PerformanceDetailCastResponse(result.id, result.name, result.role, result.photo) },
    staffList = staffs.map { result -> PerformanceDetailStaffResponse(result.id, result.name, result.role, result.photo) },
    minDueDate = minDueDate,
    performanceImageList = images.map { result -> PerformanceDetailImageResponse(result.id, result.image) },
)

internal fun BookingPerformanceDetailResult.toBookingPerformanceDetailResponse(): BookingPerformanceDetailResponse =
    BookingPerformanceDetailResponse(
        performanceId = performanceId,
        performanceTitle = performanceTitle,
        performancePeriod = performancePeriod,
        scheduleList = schedules.map {
            BookingPerformanceDetailScheduleResponse(
                scheduleId = it.scheduleId,
                performanceDate = it.performanceDate,
                scheduleNumber = it.scheduleNumber,
                availableTicketCount = it.availableTicketCount,
                isBooking = it.isBooking,
                dueDate = it.dueDate,
            )
        },
        ticketPrice = ticketPrice,
        genre = genre,
        posterImage = posterImage,
        performanceVenue = performanceVenue,
        performanceTeamName = performanceTeamName,
        bankName = bankName,
        accountNumber = accountNumber,
        accountHolder = accountHolder,
    )

internal fun MakerPerformanceListResult.toMakerPerformanceResponse(): MakerPerformanceResponse = MakerPerformanceResponse(
    userId = userId,
    performances = performances.map {
        MakerPerformanceDetailResponse(
            performanceId = it.performanceId,
            genre = it.genre,
            performanceTitle = it.performanceTitle,
            posterImage = it.posterImage,
            performancePeriod = it.performancePeriod,
            minDueDate = it.minDueDate,
        )
    },
)

private fun ScheduleResult.toScheduleResponse(): ScheduleResponse =
    ScheduleResponse(
        scheduleId = id,
        performanceDate = performanceDate,
        totalTicketCount = totalTicketCount,
        dueDate = dueDate,
        scheduleNumber = scheduleNumber?.let(ScheduleNumberType::valueOf),
    )
