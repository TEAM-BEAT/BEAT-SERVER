package com.beat.infra.persistence.booking.repository

import com.beat.domain.booking.model.BookingStatus
import com.beat.infra.persistence.booking.entity.BookingJpaEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

internal interface BookingJpaRepository : JpaRepository<BookingJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    fun lockById(@Param("id") id: Long): BookingJpaEntity?

    @Query("SELECT b.scheduleId FROM Booking b WHERE b.id IN :ids")
    fun findScheduleIdsByIds(@Param("ids") ids: Collection<Long>): List<Long>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Booking b SET b.password = :encodedPassword WHERE b.userId = :userId AND b.birthDate IS NOT NULL")
    fun replaceGuestPassword(
        @Param("userId") userId: Long?,
        @Param("encodedPassword") encodedPassword: String,
    ): Int

    fun findByUserId(userId: Long): List<BookingJpaEntity>

    fun findByBookingStatusAndCancellationDateBefore(
        bookingStatus: BookingStatus,
        cancellationDate: LocalDateTime,
    ): List<BookingJpaEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.scheduleId IN :scheduleIds AND b.bookingStatus NOT IN :excludedStatuses")
    fun findActiveBookingsForUpdate(
        @Param("scheduleIds") scheduleIds: List<Long>,
        @Param("excludedStatuses") excludedStatuses: List<BookingStatus>,
    ): List<BookingJpaEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Booking b WHERE b.scheduleId IN :scheduleIds AND b.bookingStatus IN :inactiveStatuses")
    fun deleteInactiveBookingsByScheduleIds(
        @Param("scheduleIds") scheduleIds: List<Long>,
        @Param("inactiveStatuses") inactiveStatuses: List<BookingStatus>,
    ): Int
}
