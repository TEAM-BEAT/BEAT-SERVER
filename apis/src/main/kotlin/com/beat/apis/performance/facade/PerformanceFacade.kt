package com.beat.apis.performance.facade

import com.beat.apis.performance.api.request.PerformanceRequest
import com.beat.apis.performance.api.request.PerformanceModifyRequest
import com.beat.apis.performance.api.response.BookingPerformanceDetailResponse
import com.beat.apis.performance.api.response.MakerPerformanceResponse
import com.beat.apis.performance.api.response.PerformanceDetailResponse
import com.beat.apis.performance.api.response.PerformanceModifyDetailResponse
import com.beat.apis.performance.api.response.PerformanceModifyResponse
import com.beat.apis.performance.api.response.PerformanceResponse
import com.beat.application.frontoffice.performance.maker.command.CastCreateCommand
import com.beat.application.frontoffice.performance.maker.command.CastModifyCommand
import com.beat.application.frontoffice.performance.maker.command.PerformanceBankName
import com.beat.application.frontoffice.performance.maker.command.PerformanceCreateCommand
import com.beat.application.frontoffice.performance.maker.command.PerformanceCreateCommandService
import com.beat.application.frontoffice.performance.maker.command.PerformanceDeleteCommandService
import com.beat.application.frontoffice.performance.maker.command.PerformanceGenre
import com.beat.application.frontoffice.performance.maker.command.PerformanceImageCreateCommand
import com.beat.application.frontoffice.performance.maker.command.PerformanceImageModifyCommand
import com.beat.application.frontoffice.performance.maker.command.PerformanceModifyCommand
import com.beat.application.frontoffice.performance.maker.command.PerformanceModifyCommandService
import com.beat.application.frontoffice.performance.maker.command.PerformanceScheduleNumber
import com.beat.application.frontoffice.performance.maker.command.ScheduleCreateCommand
import com.beat.application.frontoffice.performance.maker.command.ScheduleModifyCommand
import com.beat.application.frontoffice.performance.maker.command.StaffCreateCommand
import com.beat.application.frontoffice.performance.maker.command.StaffModifyCommand
import com.beat.application.frontoffice.performance.maker.query.MakerPerformanceListQueryService
import com.beat.application.frontoffice.performance.booker.query.PerformanceDetailQueryService
import com.beat.application.frontoffice.performance.maker.query.PerformanceEditFormQueryService
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
    performanceTitle = performanceTitle,
    genre = PerformanceGenre.valueOf(genre.name),
    runningTime = runningTime,
    performanceDescription = performanceDescription,
    performanceAttentionNote = performanceAttentionNote,
    bankName = bankName?.let { PerformanceBankName.valueOf(it.name) },
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
    ticketPrice = ticketPrice,
    schedules = scheduleList.map {
        ScheduleCreateCommand.of(
            it.performanceDate,
            it.totalTicketCount,
            PerformanceScheduleNumber.valueOf(it.scheduleNumber.name),
        )
    },
    casts = castList.map {
        CastCreateCommand.of(it.castName, it.castRole, it.castPhoto)
    },
    staffs = staffList.map {
        StaffCreateCommand.of(it.staffName, it.staffRole, it.staffPhoto)
    },
    images = performanceImageList.map {
        PerformanceImageCreateCommand.from(it.performanceImage)
    },
)

private fun PerformanceModifyRequest.toCommand(): PerformanceModifyCommand = PerformanceModifyCommand.of(
    performanceId = performanceId,
    performanceTitle = performanceTitle,
    genre = PerformanceGenre.valueOf(genre.name),
    runningTime = runningTime,
    performanceDescription = performanceDescription,
    performanceAttentionNote = performanceAttentionNote,
    bankName = bankName?.let { PerformanceBankName.valueOf(it.name) },
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
    ticketPrice = ticketPrice,
    schedules = scheduleModifyRequests.map {
        ScheduleModifyCommand.of(it.scheduleId, it.performanceDate, it.totalTicketCount)
    },
    casts = castModifyRequests.map {
        CastModifyCommand.of(it.castId, it.castName, it.castRole, it.castPhoto)
    },
    staffs = staffModifyRequests.map {
        StaffModifyCommand.of(it.staffId, it.staffName, it.staffRole, it.staffPhoto)
    },
    images = performanceImageModifyRequests.map {
        PerformanceImageModifyCommand.of(it.performanceImageId, it.performanceImage)
    },
)
