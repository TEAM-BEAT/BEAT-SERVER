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
import org.mockito.Mockito
import java.time.Clock
import java.time.LocalDateTime

@Tags("correctness")
class ScheduleSynchronizerSpec : FunSpec({
    lateinit var scheduleRepository: ScheduleRepository
    lateinit var bookingRepository: BookingRepository

    beforeTest {
        scheduleRepository = Mockito.mock(ScheduleRepository::class.java)
        bookingRepository = Mockito.mock(BookingRepository::class.java)
    }

    test("locks distinct schedules in sorted order before checking active bookings") {
        val scheduleIds = listOf(3L, 1L, 2L, 1L)
        Mockito.`when`(scheduleRepository.findIdsByPerformanceId(9L)).thenReturn(scheduleIds)
        Mockito.`when`(scheduleRepository.lockById(1L)).thenReturn(schedule(1L))
        Mockito.`when`(scheduleRepository.lockById(2L)).thenReturn(schedule(2L))
        Mockito.`when`(scheduleRepository.lockById(3L)).thenReturn(schedule(3L))
        Mockito.`when`(
            bookingRepository.existsActiveBookingByScheduleIds(
                scheduleIds,
                BookingStatus.inactiveForTicketAllocation(),
            ),
        ).thenReturn(false)
        val synchronizer = ScheduleSynchronizer(
            scheduleRepository,
            bookingRepository,
            ScheduleSequenceDomainService(),
            Clock.systemUTC(),
        )

        val result = synchronizer.lockAndCheckActiveBookings(9L)

        result shouldBe false
        val order = Mockito.inOrder(scheduleRepository, bookingRepository)
        order.verify(scheduleRepository).findIdsByPerformanceId(9L)
        order.verify(scheduleRepository).lockById(1L)
        order.verify(scheduleRepository).lockById(2L)
        order.verify(scheduleRepository).lockById(3L)
        order.verify(bookingRepository).existsActiveBookingByScheduleIds(
            scheduleIds,
            BookingStatus.inactiveForTicketAllocation(),
        )
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
