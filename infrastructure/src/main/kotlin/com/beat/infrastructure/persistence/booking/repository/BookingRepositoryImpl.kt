package com.beat.infrastructure.persistence.booking.repository

import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.infrastructure.persistence.booking.mapper.BookingPersistenceMapper
import java.time.LocalDateTime
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
internal class BookingRepositoryImpl(
    private val bookingJpaRepository: BookingJpaRepository,
    private val bookingPersistenceMapper: BookingPersistenceMapper,
) : BookingRepository {
    override fun save(booking: Booking): Booking =
        bookingPersistenceMapper.toDomain(
            bookingJpaRepository.save(bookingPersistenceMapper.toEntity(booking))
        )

    override fun findById(id: Long): Booking? =
        bookingJpaRepository.findByIdOrNull(id)?.let(bookingPersistenceMapper::toDomain)

    override fun findScheduleIdsByIds(ids: Collection<Long>): List<Long> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        return bookingJpaRepository.findScheduleIdsByIds(ids)
    }

    override fun lockById(id: Long): Booking? =
        bookingJpaRepository.lockById(id)?.let(bookingPersistenceMapper::toDomain)

    override fun findAll(): List<Booking> =
        bookingJpaRepository.findAll().map(bookingPersistenceMapper::toDomain)

    override fun deleteAll(bookings: Iterable<Booking>) {
        bookingJpaRepository.deleteAll(bookings.map(bookingPersistenceMapper::toEntity))
    }

    override fun findByBookingStatusAndCancellationDateBefore(
        bookingStatus: BookingStatus,
        cancellationDate: LocalDateTime,
    ): List<Booking> =
        bookingJpaRepository
            .findByBookingStatusAndCancellationDateBefore(bookingStatus, cancellationDate)
            .map(bookingPersistenceMapper::toDomain)

    override fun findByUserId(userId: Long): List<Booking> =
        bookingJpaRepository.findByUserId(userId).map(bookingPersistenceMapper::toDomain)

    override fun existsActiveBookingByScheduleIds(
        scheduleIds: List<Long>,
        excludedStatuses: List<BookingStatus>,
    ): Boolean {
        if (scheduleIds.isEmpty()) {
            return false
        }
        return bookingJpaRepository
            .findActiveBookingsForUpdate(scheduleIds, excludedStatuses)
            .isNotEmpty()
    }

    override fun deleteInactiveBookingsByScheduleIds(
        scheduleIds: List<Long>,
        inactiveStatuses: List<BookingStatus>,
    ): Int {
        if (scheduleIds.isEmpty()) {
            return 0
        }
        return bookingJpaRepository.deleteInactiveBookingsByScheduleIds(
            scheduleIds,
            inactiveStatuses,
        )
    }
}
