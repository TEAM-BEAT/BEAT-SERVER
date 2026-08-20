package com.beat.application.frontoffice.performance.maker.command

import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.schedule.service.ScheduleSequenceDomainService
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ScheduleSynchronizerTest {

    @Mock
    private lateinit var scheduleRepository: ScheduleRepository

    @Mock
    private lateinit var bookingRepository: BookingRepository

    @Test
    fun `locks distinct schedules in sorted order before checking active bookings`() {
        val scheduleIds = listOf(3L, 1L, 2L, 1L)
        Mockito.`when`(scheduleRepository.findIdsByPerformanceId(9L)).thenReturn(scheduleIds)
        Mockito.`when`(scheduleRepository.lockById(1L)).thenReturn(Optional.of(schedule(1L)))
        Mockito.`when`(scheduleRepository.lockById(2L)).thenReturn(Optional.of(schedule(2L)))
        Mockito.`when`(scheduleRepository.lockById(3L)).thenReturn(Optional.of(schedule(3L)))
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

        assertFalse(result)
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
}
