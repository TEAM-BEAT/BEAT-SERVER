package com.beat.application.system.booking.command

import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class TicketCleanupApplicationSpec : FunSpec({
    test("현재 시각 기준 1년보다 오래된 취소 Booking을 조회해 삭제한다") {
        val cancelled = cancelledBooking(1L)
        val repository = RecordingBookingRepository(listOf(cancelled))
        val service = TicketCleanupService(repository, FIXED_CLOCK)

        service.deleteOldCancelledBookings()

        repository.requestedStatus shouldBe BookingStatus.BOOKING_CANCELLED
        repository.requestedCutoff shouldBe LocalDateTime.of(2025, 8, 23, 3, 0)
        repository.deleted shouldContainExactly listOf(cancelled)
    }

    test("삭제 대상이 없어도 같은 authoritative query 결과를 repository 삭제 경계에 전달한다") {
        val repository = RecordingBookingRepository(emptyList())
        val service = TicketCleanupService(repository, FIXED_CLOCK)

        service.deleteOldCancelledBookings()

        repository.deleted shouldBe emptyList()
    }
})

private class RecordingBookingRepository(
    private val queryResult: List<Booking>,
) : BookingRepository {
    var requestedStatus: BookingStatus? = null
    var requestedCutoff: LocalDateTime? = null
    var deleted: List<Booking> = emptyList()

    override fun findByBookingStatusAndCancellationDateBefore(
        bookingStatus: BookingStatus,
        cancellationDate: LocalDateTime,
    ): List<Booking> {
        requestedStatus = bookingStatus
        requestedCutoff = cancellationDate
        return queryResult
    }

    override fun deleteAll(bookings: Iterable<Booking>) {
        deleted = bookings.toList()
    }

    override fun save(booking: Booking): Booking = booking
    override fun findById(id: Long): Booking? = null
    override fun findScheduleIdsByIds(ids: Collection<Long>): List<Long> = emptyList()
    override fun lockById(id: Long): Booking? = null
    override fun findAll(): List<Booking> = emptyList()
    override fun findByUserId(userId: Long): List<Booking> = emptyList()
    override fun existsActiveBookingByScheduleIds(
        scheduleIds: List<Long>,
        excludedStatuses: List<BookingStatus>,
    ): Boolean = false
    override fun deleteInactiveBookingsByScheduleIds(
        scheduleIds: List<Long>,
        inactiveStatuses: List<BookingStatus>,
    ): Int = 0
}

private fun cancelledBooking(id: Long): Booking = Booking.rehydrate(
    id = id,
    purchaseTicketCount = 1,
    bookerName = "booker",
    bookerPhoneNumber = "010-0000-0000",
    bookingStatus = BookingStatus.BOOKING_CANCELLED,
    createdAt = LocalDateTime.of(2024, 1, 1, 0, 0),
    cancellationDate = LocalDateTime.of(2024, 8, 1, 0, 0),
    birthDate = null,
    password = null,
    refundAccount = null,
    scheduleId = 1L,
    userId = 1L,
    totalPaymentAmount = 10_000,
)

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2026-08-23T03:00:00Z"),
    ZoneOffset.UTC,
)
