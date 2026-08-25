package com.beat.domain.booking.repository

import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import java.time.LocalDateTime

interface BookingRepository {
    fun save(booking: Booking): Booking

    fun findById(id: Long): Booking?

    fun findScheduleIdsByIds(ids: Collection<Long>): List<Long>

    fun lockById(id: Long): Booking?

    fun findAll(): List<Booking>

    fun deleteAll(bookings: Iterable<Booking>)

    fun findByBookingStatusAndCancellationDateBefore(
        bookingStatus: BookingStatus,
        cancellationDate: LocalDateTime,
    ): List<Booking>

    fun findByUserId(userId: Long): List<Booking>

    fun existsActiveBookingByScheduleIds(
        scheduleIds: List<Long>,
        excludedStatuses: List<BookingStatus>,
    ): Boolean

    fun deleteInactiveBookingsByScheduleIds(
        scheduleIds: List<Long>,
        inactiveStatuses: List<BookingStatus>,
    ): Int
}
