package com.beat.infra.persistence.booking.repository

import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.infra.persistence.booking.mapper.BookingPersistenceMapper
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
class BookingRepositoryImpl(
    private val bookingJpaRepository: BookingJpaRepository,
    private val bookingPersistenceMapper: BookingPersistenceMapper,
) : BookingRepository {
    override fun save(booking: Booking): Booking =
        bookingPersistenceMapper.toDomain(
            bookingJpaRepository.save(bookingPersistenceMapper.toEntity(booking)),
        )

    override fun findById(id: Long?): Optional<Booking> =
        bookingJpaRepository.findById(requireNotNull(id) { "The given id must not be null" })
            .map(bookingPersistenceMapper::toDomain)

    override fun findScheduleIdsByIds(ids: Collection<Long>): List<Long> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        return bookingJpaRepository.findScheduleIdsByIds(ids)
    }

    override fun lockById(id: Long?): Optional<Booking> =
        bookingJpaRepository.lockById(id).map(bookingPersistenceMapper::toDomain)

    override fun findAll(): List<Booking> =
        bookingJpaRepository.findAll().map(bookingPersistenceMapper::toDomain)

    override fun deleteAll(bookings: Iterable<Booking>) {
        bookingJpaRepository.deleteAll(bookings.map(bookingPersistenceMapper::toEntity))
    }

    override fun findByBookingStatusAndCancellationDateBefore(
        bookingStatus: BookingStatus,
        cancellationDate: LocalDateTime,
    ): List<Booking> =
        bookingJpaRepository.findByBookingStatusAndCancellationDateBefore(bookingStatus, cancellationDate)
            .map(bookingPersistenceMapper::toDomain)

    override fun replaceGuestPassword(userId: Long, encodedPassword: String): Int =
        bookingJpaRepository.replaceGuestPassword(userId, encodedPassword)

    override fun findByUserId(userId: Long?): List<Booking> =
        bookingJpaRepository.findByUserId(userId).map(bookingPersistenceMapper::toDomain)

    override fun existsActiveBookingByScheduleIds(
        scheduleIds: List<Long>,
        excludedStatuses: List<BookingStatus>,
    ): Boolean {
        if (scheduleIds.isEmpty()) {
            return false
        }
        return bookingJpaRepository.existsActiveBookingByScheduleIds(scheduleIds, excludedStatuses)
    }

    override fun deleteInactiveBookingsByScheduleIds(
        scheduleIds: List<Long>,
        inactiveStatuses: List<BookingStatus>,
    ): Int {
        if (scheduleIds.isEmpty()) {
            return 0
        }
        return bookingJpaRepository.deleteInactiveBookingsByScheduleIds(scheduleIds, inactiveStatuses)
    }
}
