package com.beat.infrastructure.persistence.query.booking.booker

import com.beat.application.frontoffice.booking.booker.BookingHistoryPerformanceSnapshot
import com.beat.application.frontoffice.booking.booker.BookingHistoryReadPort
import com.beat.application.frontoffice.booking.booker.BookingHistoryScheduleSnapshot
import com.beat.application.frontoffice.booking.booker.BookingHistorySnapshot
import com.beat.infrastructure.jooq.generated.Booking
import com.beat.infrastructure.jooq.generated.Performance
import com.beat.infrastructure.jooq.generated.Schedule
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
internal class BookerBookingQueries(
    private val dsl: DSLContext,
) : BookingHistoryReadPort {

    override fun findByUserId(userId: Long): List<BookingHistorySnapshot> {
        val bookings = findBookings(userId)
        if (bookings.isEmpty()) return emptyList()

        val schedules = findSchedules(bookings.map { it.scheduleId }.distinct())
            .associateBy { it.scheduleId }

        val performances = if (schedules.isEmpty()) {
            emptyMap()
        } else {
            findPerformances(schedules.values.map { it.performanceId }.distinct())
                .associateBy { it.performanceId }
        }

        return bookings.map { booking ->
            val schedule = schedules[booking.scheduleId]
            BookingHistorySnapshot(
                userId = booking.userId,
                bookingId = checkNotNull(booking.bookingId),
                purchaseTicketCount = booking.purchaseTicketCount,
                bookerName = booking.bookerName,
                bookingStatus = booking.bookingStatus,
                createdAt = booking.createdAt,
                totalPaymentAmount = booking.totalPaymentAmount,
                schedule = schedule?.toSnapshot(),
                performance = schedule?.let { performances[it.performanceId]?.toSnapshot() },
            )
        }
    }

    private fun findBookings(userId: Long): List<BookingProjection> =
        dsl.select(
            Booking.ID,
            Booking.USER_ID,
            Booking.SCHEDULE_ID,
            Booking.PURCHASE_TICKET_COUNT,
            Booking.BOOKER_NAME,
            Booking.BOOKING_STATUS,
            Booking.CREATED_AT,
            Booking.TOTAL_PAYMENT_AMOUNT,
        ).from(Booking.TABLE)
            .where(Booking.USER_ID.eq(userId))
            .fetch { record ->
                BookingProjection(
                    bookingId = record.get(Booking.ID),
                    userId = record.get(Booking.USER_ID)!!,
                    scheduleId = record.get(Booking.SCHEDULE_ID)!!,
                    purchaseTicketCount = record.get(Booking.PURCHASE_TICKET_COUNT)!!,
                    bookerName = record.get(Booking.BOOKER_NAME)!!,
                    bookingStatus = record.get(Booking.BOOKING_STATUS)!!,
                    createdAt = record.get(Booking.CREATED_AT)!!,
                    totalPaymentAmount = record.get(Booking.TOTAL_PAYMENT_AMOUNT),
                )
            }

    private fun findSchedules(scheduleIds: Collection<Long>): List<ScheduleProjection> {
        if (scheduleIds.isEmpty()) return emptyList()
        return dsl.select(
            Schedule.ID,
            Schedule.PERFORMANCE_ID,
            Schedule.PERFORMANCE_DATE,
            Schedule.SCHEDULE_NUMBER,
        ).from(Schedule.TABLE)
            .where(Schedule.ID.`in`(scheduleIds))
            .fetch { record ->
                ScheduleProjection(
                    scheduleId = record.get(Schedule.ID),
                    performanceId = record.get(Schedule.PERFORMANCE_ID)!!,
                    performanceDate = record.get(Schedule.PERFORMANCE_DATE)!!,
                    scheduleNumber = record.get(Schedule.SCHEDULE_NUMBER)!!,
                )
            }
    }

    private fun findPerformances(performanceIds: Collection<Long>): List<PerformanceProjection> {
        if (performanceIds.isEmpty()) return emptyList()
        return dsl.select(
            Performance.ID,
            Performance.PERFORMANCE_TITLE,
            Performance.PERFORMANCE_VENUE,
            Performance.PERFORMANCE_CONTACT,
            Performance.BANK_NAME,
            Performance.ACCOUNT_NUMBER,
            Performance.ACCOUNT_HOLDER,
            Performance.POSTER_IMAGE,
            Performance.TICKET_PRICE,
        ).from(Performance.TABLE)
            .where(Performance.ID.`in`(performanceIds))
            .fetch { record ->
                PerformanceProjection(
                    performanceId = record.get(Performance.ID),
                    performanceTitle = record.get(Performance.PERFORMANCE_TITLE)!!,
                    performanceVenue = record.get(Performance.PERFORMANCE_VENUE)!!,
                    performanceContact = record.get(Performance.PERFORMANCE_CONTACT)!!,
                    bankName = record.get(Performance.BANK_NAME),
                    accountNumber = record.get(Performance.ACCOUNT_NUMBER),
                    accountHolder = record.get(Performance.ACCOUNT_HOLDER),
                    posterImage = record.get(Performance.POSTER_IMAGE)!!,
                    ticketPrice = record.get(Performance.TICKET_PRICE)!!,
                )
            }
    }

    private fun ScheduleProjection.toSnapshot(): BookingHistoryScheduleSnapshot =
        BookingHistoryScheduleSnapshot(
            scheduleId = checkNotNull(scheduleId),
            performanceId = performanceId,
            performanceDate = performanceDate,
            scheduleNumber = scheduleNumber,
        )

    private fun PerformanceProjection.toSnapshot(): BookingHistoryPerformanceSnapshot =
        BookingHistoryPerformanceSnapshot(
            performanceId = checkNotNull(performanceId),
            performanceTitle = performanceTitle,
            performanceVenue = performanceVenue,
            performanceContact = performanceContact,
            bankName = bankName,
            accountNumber = accountNumber,
            accountHolder = accountHolder,
            posterImage = posterImage,
            ticketPrice = ticketPrice,
        )

    private data class BookingProjection(
        val bookingId: Long?,
        val userId: Long,
        val scheduleId: Long,
        val purchaseTicketCount: Int,
        val bookerName: String,
        val bookingStatus: String,
        val createdAt: java.time.LocalDateTime,
        val totalPaymentAmount: Int?,
    )

    private data class ScheduleProjection(
        val scheduleId: Long?,
        val performanceId: Long,
        val performanceDate: java.time.LocalDateTime,
        val scheduleNumber: String,
    )

    private data class PerformanceProjection(
        val performanceId: Long?,
        val performanceTitle: String,
        val performanceVenue: String,
        val performanceContact: String,
        val bankName: String?,
        val accountNumber: String?,
        val accountHolder: String?,
        val posterImage: String,
        val ticketPrice: Int,
    )
}
