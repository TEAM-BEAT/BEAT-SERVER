package com.beat.domain.booking.repository

import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import java.time.LocalDateTime
import java.util.*

@JvmSuppressWildcards
interface BookingRepository {
    fun save(booking: Booking): Booking

    fun findById(id: Long?): Optional<Booking>

    fun findAllById(ids: Collection<Long>): List<Booking>

    fun lockById(id: Long?): Optional<Booking>

    fun findAll(): List<Booking>

    fun deleteAll(bookings: Iterable<Booking>)

    fun findByBookingStatusAndCancellationDateBefore(
        bookingStatus: BookingStatus,
        cancellationDate: LocalDateTime,
    ): List<Booking>

    fun replaceGuestPassword(userId: Long, encodedPassword: String): Int

    fun findByUserId(userId: Long?): List<Booking>

    fun existsActiveBookingByScheduleIds(
        scheduleIds: List<Long>,
        excludedStatuses: List<BookingStatus>,
    ): Boolean

    fun deleteInactiveBookingsByScheduleIds(
        scheduleIds: List<Long>,
        inactiveStatuses: List<BookingStatus>,
    ): Int
}
