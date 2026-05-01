package com.beat.domain.booking.repository

import com.beat.domain.booking.domain.Booking
import com.beat.domain.booking.domain.BookingStatus
import java.time.LocalDateTime
import java.util.*

@JvmSuppressWildcards
interface BookingRepository {
    fun save(booking: Booking): Booking

    fun findById(id: Long?): Optional<Booking>

    fun findAll(): List<Booking>

    fun deleteAll(bookings: Iterable<Booking>)

    fun findByBookingStatusAndCancellationDateBefore(
        bookingStatus: BookingStatus,
        cancellationDate: LocalDateTime,
    ): List<Booking>

    fun findByBookerNameAndBookerPhoneNumberAndPasswordAndBirthDate(
        bookerName: String,
        bookerPhoneNumber: String,
        password: String,
        birthDate: String,
    ): Optional<List<Booking>>

    fun findFirstByBookerNameAndBookerPhoneNumberAndBirthDateAndPassword(
        bookerName: String,
        bookerPhoneNumber: String,
        birthDate: String,
        password: String,
    ): Optional<Booking>

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
