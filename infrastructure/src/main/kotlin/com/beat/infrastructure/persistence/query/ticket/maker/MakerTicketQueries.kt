package com.beat.infrastructure.persistence.query.ticket.maker

import com.beat.application.frontoffice.ticket.maker.query.MakerTicketBookingStatus
import com.beat.application.frontoffice.ticket.maker.query.MakerTicketListItemReadModel
import com.beat.application.frontoffice.ticket.maker.query.MakerTicketReader
import com.beat.application.frontoffice.ticket.maker.query.MakerTicketScheduleNumber
import com.beat.application.frontoffice.ticket.maker.query.MakerTicketScheduleReadModel
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.infrastructure.jooq.generated.Booking as BookingTable
import com.beat.infrastructure.jooq.generated.Schedule as ScheduleTable
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
internal class MakerTicketQueries(
    private val dsl: DSLContext,
) : MakerTicketReader {

    override fun findTickets(
        performanceId: Long,
        scheduleNumbers: List<MakerTicketScheduleNumber>,
        bookingStatuses: List<MakerTicketBookingStatus>,
    ): List<MakerTicketListItemReadModel> =
        queryTickets(performanceId, scheduleNumbers, bookingStatuses, searchWord = null)

    override fun searchTickets(
        performanceId: Long,
        searchWord: String,
        scheduleNumbers: List<MakerTicketScheduleNumber>,
        bookingStatuses: List<MakerTicketBookingStatus>,
    ): List<MakerTicketListItemReadModel> {
        if (searchWord.isBlank()) {
            return emptyList()
        }
        return queryTickets(performanceId, scheduleNumbers, bookingStatuses, searchWord)
    }

    override fun findSchedules(performanceId: Long): List<MakerTicketScheduleReadModel> =
        dsl.select(
            ScheduleTable.ID,
            ScheduleTable.TOTAL_TICKET_COUNT,
            ScheduleTable.SOLD_TICKET_COUNT,
            ScheduleTable.SCHEDULE_NUMBER,
        ).from(ScheduleTable.TABLE)
            .where(ScheduleTable.PERFORMANCE_ID.eq(performanceId))
            .fetch { record ->
                MakerTicketScheduleReadModel(
                    scheduleId = record.get(ScheduleTable.ID)!!,
                    totalTicketCount = record.get(ScheduleTable.TOTAL_TICKET_COUNT)!!,
                    soldTicketCount = record.get(ScheduleTable.SOLD_TICKET_COUNT)!!,
                    scheduleNumber = record.get(ScheduleTable.SCHEDULE_NUMBER)!!,
                )
            }

    private fun queryTickets(
        performanceId: Long,
        scheduleNumbers: List<MakerTicketScheduleNumber>,
        bookingStatuses: List<MakerTicketBookingStatus>,
        searchWord: String?,
    ): List<MakerTicketListItemReadModel> {
        val conditions = mutableListOf<Condition>()
        conditions.add(BookingTable.BOOKING_STATUS.ne(BookingStatus.BOOKING_DELETED.name))
        conditions.add(ScheduleTable.PERFORMANCE_ID.eq(performanceId))

        val scheduleNumberStrings = scheduleNumbers.map { it.name }
        if (scheduleNumberStrings.isNotEmpty()) {
            conditions.add(ScheduleTable.SCHEDULE_NUMBER.`in`(scheduleNumberStrings))
        }

        val bookingStatusStrings = bookingStatuses.map { it.name }
        if (bookingStatusStrings.isNotEmpty()) {
            conditions.add(BookingTable.BOOKING_STATUS.`in`(bookingStatusStrings))
        }

        if (searchWord != null) {
            // MySQL full-text : MATCH(booker_name) AGAINST (? IN BOOLEAN MODE) > 0
            // Original JDSL: function(Double, "match", path(bookerName), value(searchWord)).gt(0.0)
            // Replicate with plain SQL condition for MySQL.
            conditions.add(
                DSL.condition(
                    "MATCH({0}) AGAINST ({1} IN BOOLEAN MODE) > 0",
                    BookingTable.BOOKER_NAME,
                    DSL.inline(searchWord),
                ),
            )
        }

        // Ordering: REFUND_REQUESTED(1) < CHECKING_PAYMENT(2) < BOOKING_CONFIRMED(3) < BOOKING_CANCELLED(4) < else 5, then created_at desc
        val orderingField = DSL.case_()
            .`when`(BookingTable.BOOKING_STATUS.eq(BookingStatus.REFUND_REQUESTED.name), 1)
            .`when`(BookingTable.BOOKING_STATUS.eq(BookingStatus.CHECKING_PAYMENT.name), 2)
            .`when`(BookingTable.BOOKING_STATUS.eq(BookingStatus.BOOKING_CONFIRMED.name), 3)
            .`when`(BookingTable.BOOKING_STATUS.eq(BookingStatus.BOOKING_CANCELLED.name), 4)
            .otherwise(5)

        return dsl.select(
            BookingTable.ID,
            BookingTable.BOOKER_NAME,
            BookingTable.BOOKER_PHONE_NUMBER,
            BookingTable.SCHEDULE_ID,
            BookingTable.PURCHASE_TICKET_COUNT,
            BookingTable.CREATED_AT,
            BookingTable.BOOKING_STATUS,
            BookingTable.TOTAL_PAYMENT_AMOUNT,
            BookingTable.BANK_NAME,
            BookingTable.ACCOUNT_NUMBER,
            BookingTable.ACCOUNT_HOLDER,
        ).from(BookingTable.TABLE)
            .join(ScheduleTable.TABLE).on(BookingTable.SCHEDULE_ID.eq(ScheduleTable.ID))
            .where(conditions)
            .orderBy(orderingField.asc(), BookingTable.CREATED_AT.desc())
            .fetch { record ->
                val bookingStatusStr = record.get(BookingTable.BOOKING_STATUS)!!
                val bookingStatus = try {
                    BookingStatus.valueOf(bookingStatusStr)
                } catch (_: Exception) {
                    BookingStatus.BOOKING_CONFIRMED
                }
                MakerTicketListItemReadModel(
                    bookingId = record.get(BookingTable.ID)!!,
                    bookerName = record.get(BookingTable.BOOKER_NAME)!!,
                    bookerPhoneNumber = record.get(BookingTable.BOOKER_PHONE_NUMBER)!!,
                    scheduleId = record.get(BookingTable.SCHEDULE_ID)!!,
                    purchaseTicketCount = record.get(BookingTable.PURCHASE_TICKET_COUNT)!!,
                    createdAt = record.get(BookingTable.CREATED_AT)!!,
                    bookingStatus = MakerTicketBookingStatus.valueOf(bookingStatus.name),
                    bankName = (record.get(BookingTable.BANK_NAME)?.let {
                        try {
                            com.beat.domain.sharedkernel.vo.BankName.valueOf(it)
                        } catch (_: Exception) {
                            com.beat.domain.sharedkernel.vo.BankName.NONE
                        }
                    } ?: com.beat.domain.sharedkernel.vo.BankName.NONE).displayName,
                    accountNumber = record.get(BookingTable.ACCOUNT_NUMBER) ?: "",
                    accountHolder = record.get(BookingTable.ACCOUNT_HOLDER) ?: "",
                    deletable = Booking.canDeleteByMaker(bookingStatus, record.get(BookingTable.TOTAL_PAYMENT_AMOUNT)),
                )
            }
    }
}
