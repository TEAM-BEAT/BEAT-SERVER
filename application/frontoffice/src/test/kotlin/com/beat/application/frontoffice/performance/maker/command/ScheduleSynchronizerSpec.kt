package com.beat.application.frontoffice.performance.maker.command

import com.beat.application.frontoffice.fixture.frontofficeScheduleFixture
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.schedule.service.ScheduleSequenceDomainService
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Tags("correctness")
class ScheduleSynchronizerSpec : FunSpec({
    test("활성 예매 확인 전에 정렬된 순서로 서로 다른 회차를 잠근다") {
        val scheduleRepository = mockk<ScheduleRepository>(relaxed = true)
        val bookingRepository = mockk<BookingRepository>(relaxed = true)
        val scheduleIds = listOf(3L, 1L, 2L, 1L)
        every { scheduleRepository.findIdsByPerformanceId(9L) } returns scheduleIds
        every { scheduleRepository.lockById(1L) } returns frontofficeScheduleFixture(id = 1L, performanceId = 9L)
        every { scheduleRepository.lockById(2L) } returns frontofficeScheduleFixture(id = 2L, performanceId = 9L)
        every { scheduleRepository.lockById(3L) } returns frontofficeScheduleFixture(id = 3L, performanceId = 9L)
        every {
            bookingRepository.existsActiveBookingByScheduleIds(
                scheduleIds,
                BookingStatus.inactiveForTicketAllocation(),
            )
        } returns false
        val synchronizer = ScheduleSynchronizer(
            scheduleRepository,
            bookingRepository,
            ScheduleSequenceDomainService(),
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
        )

        val result = synchronizer.lockAndCheckActiveBookings(9L)

        result shouldBe false
        verifyOrder {
            scheduleRepository.findIdsByPerformanceId(9L)
            scheduleRepository.lockById(1L)
            scheduleRepository.lockById(2L)
            scheduleRepository.lockById(3L)
            bookingRepository.existsActiveBookingByScheduleIds(
                scheduleIds,
                BookingStatus.inactiveForTicketAllocation(),
            )
        }
    }

})
