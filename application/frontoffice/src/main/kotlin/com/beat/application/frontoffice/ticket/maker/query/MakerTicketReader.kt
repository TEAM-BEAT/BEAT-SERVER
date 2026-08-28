package com.beat.application.frontoffice.ticket.maker.query

import com.beat.application.frontoffice.query.PresentationReadModel
import java.time.LocalDateTime

@PresentationReadModel
interface MakerTicketReader {
    fun findTickets(
        performanceId: Long,
        scheduleNumbers: List<MakerTicketScheduleNumber>,
        bookingStatuses: List<MakerTicketBookingStatus>,
    ): List<MakerTicketListItemReadModel>

    fun searchTickets(
        performanceId: Long,
        searchWord: String,
        scheduleNumbers: List<MakerTicketScheduleNumber>,
        bookingStatuses: List<MakerTicketBookingStatus>,
    ): List<MakerTicketListItemReadModel>

    fun findSchedules(performanceId: Long): List<MakerTicketScheduleReadModel>
}

@PresentationReadModel
data class MakerTicketScheduleReadModel(
    val scheduleId: Long,
    val totalTicketCount: Int,
    val soldTicketCount: Int,
    val scheduleNumber: String,
)

@PresentationReadModel
data class MakerTicketListItemReadModel(
    val bookingId: Long,
    val bookerName: String,
    val bookerPhoneNumber: String,
    val scheduleId: Long,
    val purchaseTicketCount: Int,
    val createdAt: LocalDateTime,
    val bookingStatus: MakerTicketBookingStatus,
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
    val deletable: Boolean,
)
