package com.beat.apis.performance.facade

import com.beat.apis.performance.api.request.PerformanceRequest
import com.beat.apis.performance.api.request.PerformanceModifyRequest
import com.beat.apis.performance.api.response.BookingPerformanceDetailResponse
import com.beat.apis.performance.api.response.MakerPerformanceResponse
import com.beat.apis.performance.api.response.PerformanceDetailResponse
import com.beat.apis.performance.api.response.PerformanceModifyDetailResponse
import com.beat.apis.performance.api.response.PerformanceModifyResponse
import com.beat.apis.performance.api.response.PerformanceResponse
import com.beat.apis.performance.api.response.toBookingPerformanceDetailResponse
import com.beat.apis.performance.api.response.toMakerPerformanceResponse
import com.beat.apis.performance.api.response.toPerformanceDetailResponse
import com.beat.apis.performance.api.response.toPerformanceModifyDetailResponse
import com.beat.apis.performance.api.response.toPerformanceModifyResponse
import com.beat.apis.performance.api.response.toPerformanceResponse
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
        createCommandService.createPerformance(memberId, request.toCommand()).toPerformanceResponse()

    fun modifyPerformance(memberId: Long, request: PerformanceModifyRequest): PerformanceModifyResponse =
        modifyCommandService.modifyPerformance(memberId, request.toCommand()).toPerformanceModifyResponse()

    fun getPerformanceEdit(memberId: Long, performanceId: Long): PerformanceModifyDetailResponse =
        editFormQueryService.getPerformanceEdit(memberId, performanceId).toPerformanceModifyDetailResponse()

    fun getPerformanceDetail(performanceId: Long): PerformanceDetailResponse =
        detailQueryService.getPerformanceDetail(performanceId).toPerformanceDetailResponse()

    fun getBookingPerformanceDetail(performanceId: Long): BookingPerformanceDetailResponse =
        detailQueryService.getBookingPerformanceDetail(performanceId).toBookingPerformanceDetailResponse()

    fun getMemberPerformances(memberId: Long): MakerPerformanceResponse =
        makerPerformanceListQueryService.getMemberPerformances(memberId).toMakerPerformanceResponse()

    fun deletePerformance(memberId: Long, performanceId: Long) {
        deleteCommandService.deletePerformance(memberId, performanceId)
    }
}

private fun PerformanceRequest.toCommand(): PerformanceCreateCommand = PerformanceCreateCommand(
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
        ScheduleCreateCommand(
            requireNotNull(it.performanceDate),
            requireNotNull(it.totalTicketCount),
            PerformanceScheduleNumber.valueOf(requireNotNull(it.scheduleNumber).name),
        )
    },
    casts = requireNotNull(castList).map {
        CastCreateCommand(requireNotNull(it.castName), requireNotNull(it.castRole), requireNotNull(it.castPhoto))
    },
    staffs = requireNotNull(staffList).map {
        StaffCreateCommand(requireNotNull(it.staffName), requireNotNull(it.staffRole), requireNotNull(it.staffPhoto))
    },
    images = requireNotNull(performanceImageList).map {
        PerformanceImageCreateCommand(requireNotNull(it.performanceImage))
    },
)

private fun PerformanceModifyRequest.toCommand(): PerformanceModifyCommand = PerformanceModifyCommand(
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
        ScheduleModifyCommand(it.scheduleId, requireNotNull(it.performanceDate), requireNotNull(it.totalTicketCount))
    },
    casts = requireNotNull(castModifyRequests).map {
        CastModifyCommand(it.castId, requireNotNull(it.castName), requireNotNull(it.castRole), requireNotNull(it.castPhoto))
    },
    staffs = requireNotNull(staffModifyRequests).map {
        StaffModifyCommand(it.staffId, requireNotNull(it.staffName), requireNotNull(it.staffRole), requireNotNull(it.staffPhoto))
    },
    images = requireNotNull(performanceImageModifyRequests).map {
        PerformanceImageModifyCommand(it.performanceImageId, requireNotNull(it.performanceImage))
    },
)
