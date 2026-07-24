package com.beat.apis.performance.facade

import com.beat.apis.performance.api.request.PerformanceRequest
import com.beat.apis.performance.api.request.PerformanceModifyRequest
import com.beat.apis.performance.api.response.BookingPerformanceDetailResponse
import com.beat.apis.performance.api.response.MakerPerformanceResponse
import com.beat.apis.performance.api.response.PerformanceDetailResponse
import com.beat.apis.performance.api.response.PerformanceModifyDetailResponse
import com.beat.apis.performance.api.response.PerformanceModifyResponse
import com.beat.apis.performance.api.response.PerformanceResponse
import com.beat.apis.performance.application.command.CastCreateCommand
import com.beat.apis.performance.application.command.CastModifyCommand
import com.beat.apis.performance.application.command.PerformanceBankName
import com.beat.apis.performance.application.command.PerformanceCreateCommand
import com.beat.apis.performance.application.command.PerformanceCreateCommandService
import com.beat.apis.performance.application.command.PerformanceDeleteCommandService
import com.beat.apis.performance.application.command.PerformanceGenre
import com.beat.apis.performance.application.command.PerformanceImageCreateCommand
import com.beat.apis.performance.application.command.PerformanceImageModifyCommand
import com.beat.apis.performance.application.command.PerformanceModifyCommand
import com.beat.apis.performance.application.command.PerformanceModifyCommandService
import com.beat.apis.performance.application.command.PerformanceScheduleNumber
import com.beat.apis.performance.application.command.ScheduleCreateCommand
import com.beat.apis.performance.application.command.ScheduleModifyCommand
import com.beat.apis.performance.application.command.StaffCreateCommand
import com.beat.apis.performance.application.command.StaffModifyCommand
import com.beat.apis.performance.application.query.MakerPerformanceListQueryService
import com.beat.apis.performance.application.query.PerformanceDetailQueryService
import com.beat.apis.performance.application.query.PerformanceEditFormQueryService
import org.springframework.stereotype.Service

@Service
class PerformanceFacade(
    private val detailQueryService: PerformanceDetailQueryService,
    private val editFormQueryService: PerformanceEditFormQueryService,
    private val makerPerformanceListQueryService: MakerPerformanceListQueryService,
    private val createCommandService: PerformanceCreateCommandService,
    private val deleteCommandService: PerformanceDeleteCommandService,
    private val modifyCommandService: PerformanceModifyCommandService,
) {
    fun createPerformance(memberId: Long, request: PerformanceRequest): PerformanceResponse =
        PerformanceResponse.from(createCommandService.createPerformance(memberId, request.toCommand()))

    fun modifyPerformance(memberId: Long, request: PerformanceModifyRequest): PerformanceModifyResponse =
        PerformanceModifyResponse.from(modifyCommandService.modifyPerformance(memberId, request.toCommand()))

    fun getPerformanceEdit(memberId: Long, performanceId: Long): PerformanceModifyDetailResponse =
        PerformanceModifyDetailResponse.from(editFormQueryService.getPerformanceEdit(memberId, performanceId))

    fun getPerformanceDetail(performanceId: Long): PerformanceDetailResponse =
        PerformanceDetailResponse.from(detailQueryService.getPerformanceDetail(performanceId))

    fun getBookingPerformanceDetail(performanceId: Long): BookingPerformanceDetailResponse =
        BookingPerformanceDetailResponse.from(detailQueryService.getBookingPerformanceDetail(performanceId))

    fun getMemberPerformances(memberId: Long): MakerPerformanceResponse =
        MakerPerformanceResponse.from(makerPerformanceListQueryService.getMemberPerformances(memberId))

    fun deletePerformance(memberId: Long, performanceId: Long) {
        deleteCommandService.deletePerformance(memberId, performanceId)
    }
}

private fun PerformanceRequest.toCommand(): PerformanceCreateCommand = PerformanceCreateCommand.of(
    performanceTitle = requireNotNull(performanceTitle),
    genre = PerformanceGenre.valueOf(requireNotNull(genre).name),
    runningTime = requireNotNull(runningTime),
    performanceDescription = requireNotNull(performanceDescription),
    performanceAttentionNote = requireNotNull(performanceAttentionNote),
    bankName = bankName?.let { PerformanceBankName.valueOf(it.name) },
    accountNumber = accountNumber,
    accountHolder = accountHolder,
    posterImage = requireNotNull(posterImage),
    performanceTeamName = requireNotNull(performanceTeamName),
    performanceVenue = requireNotNull(performanceVenue),
    roadAddressName = requireNotNull(roadAddressName),
    placeDetailAddress = requireNotNull(placeDetailAddress),
    latitude = requireNotNull(latitude),
    longitude = requireNotNull(longitude),
    performanceContact = requireNotNull(performanceContact),
    ticketPrice = requireNotNull(ticketPrice),
    schedules = requireNotNull(scheduleList).map {
        ScheduleCreateCommand.of(
            requireNotNull(it.performanceDate),
            requireNotNull(it.totalTicketCount),
            PerformanceScheduleNumber.valueOf(requireNotNull(it.scheduleNumber).name),
        )
    },
    casts = requireNotNull(castList).map {
        CastCreateCommand.of(requireNotNull(it.castName), requireNotNull(it.castRole), requireNotNull(it.castPhoto))
    },
    staffs = requireNotNull(staffList).map {
        StaffCreateCommand.of(requireNotNull(it.staffName), requireNotNull(it.staffRole), requireNotNull(it.staffPhoto))
    },
    images = requireNotNull(performanceImageList).map {
        PerformanceImageCreateCommand.from(requireNotNull(it.performanceImage))
    },
)

private fun PerformanceModifyRequest.toCommand(): PerformanceModifyCommand = PerformanceModifyCommand.of(
    performanceId = requireNotNull(performanceId),
    performanceTitle = requireNotNull(performanceTitle),
    genre = PerformanceGenre.valueOf(requireNotNull(genre).name),
    runningTime = requireNotNull(runningTime),
    performanceDescription = requireNotNull(performanceDescription),
    performanceAttentionNote = requireNotNull(performanceAttentionNote),
    bankName = bankName?.let { PerformanceBankName.valueOf(it.name) },
    accountNumber = accountNumber,
    accountHolder = accountHolder,
    posterImage = requireNotNull(posterImage),
    performanceTeamName = requireNotNull(performanceTeamName),
    performanceVenue = requireNotNull(performanceVenue),
    roadAddressName = requireNotNull(roadAddressName),
    placeDetailAddress = requireNotNull(placeDetailAddress),
    latitude = requireNotNull(latitude),
    longitude = requireNotNull(longitude),
    performanceContact = requireNotNull(performanceContact),
    ticketPrice = requireNotNull(ticketPrice),
    schedules = requireNotNull(scheduleModifyRequests).map {
        ScheduleModifyCommand.of(it.scheduleId, requireNotNull(it.performanceDate), requireNotNull(it.totalTicketCount))
    },
    casts = requireNotNull(castModifyRequests).map {
        CastModifyCommand.of(it.castId, requireNotNull(it.castName), requireNotNull(it.castRole), requireNotNull(it.castPhoto))
    },
    staffs = requireNotNull(staffModifyRequests).map {
        StaffModifyCommand.of(it.staffId, requireNotNull(it.staffName), requireNotNull(it.staffRole), requireNotNull(it.staffPhoto))
    },
    images = requireNotNull(performanceImageModifyRequests).map {
        PerformanceImageModifyCommand.of(it.performanceImageId, requireNotNull(it.performanceImage))
    },
)
