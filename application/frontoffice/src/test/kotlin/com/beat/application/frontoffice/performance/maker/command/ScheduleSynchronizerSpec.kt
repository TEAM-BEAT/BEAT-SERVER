package com.beat.application.frontoffice.performance.maker.command

import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.schedule.service.ScheduleSequenceDomainService
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import java.time.Clock
import java.time.LocalDateTime

@Tags("correctness")
class ScheduleSynchronizerSpec : FunSpec({
    lateinit var scheduleRepository: ScheduleRepository
    lateinit var bookingRepository: BookingRepository

    beforeTest {
        scheduleRepository = mockk(relaxed = true)
        bookingRepository = mockk(relaxed = true)
    }

    test("활성 예매 확인 전에 정렬된 순서로 서로 다른 회차를 잠근다") {
        val scheduleIds = listOf(3L, 1L, 2L, 1L)
        every { scheduleRepository.findIdsByPerformanceId(9L) } returns scheduleIds
        every { scheduleRepository.lockById(1L) } returns schedule(1L)
        every { scheduleRepository.lockById(2L) } returns schedule(2L)
        every { scheduleRepository.lockById(3L) } returns schedule(3L)
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
            Clock.systemUTC(),
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

private fun schedule(id: Long): Schedule {
    val performanceDate = LocalDateTime.of(2026, 1, 2, 12, 0)
    return Schedule.rehydrate(
        id,
        performanceDate,
        performanceDate.plusHours(1),
        10,
        0,
        ScheduleNumber.FIRST,
        9L,
    )
}
