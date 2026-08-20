package com.beat.application.frontoffice.performance.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.performance.exception.PerformanceApplicationErrorCode
import com.beat.application.frontoffice.performance.maker.ScheduleResult
import com.beat.application.frontoffice.schedule.calculateDueDate
import com.beat.application.frontoffice.schedule.exception.ScheduleApplicationErrorCode
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.performance.model.Performance
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.schedule.service.ScheduleSequenceDomainService
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Clock

@Component
internal class ScheduleSynchronizer(
    private val scheduleRepository: ScheduleRepository,
    private val bookingRepository: BookingRepository,
    private val scheduleSequenceDomainService: ScheduleSequenceDomainService,
    private val clock: Clock,
) {
    fun lockAndCheckActiveBookings(performanceId: Long): Boolean {
        val scheduleIds = scheduleRepository.findIdsByPerformanceId(performanceId)
        scheduleIds.distinct().sorted().forEach { scheduleId ->
            scheduleRepository.lockById(scheduleId)
                .orElseThrow { FrontofficeApplicationException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND) }
        }
        return bookingRepository.existsActiveBookingByScheduleIds(
            scheduleIds,
            BookingStatus.inactiveForTicketAllocation(),
        )
    }

    fun synchronize(
        requests: List<ScheduleModifyCommand>,
        performance: Performance,
    ): List<ScheduleResult> {
        val performanceId = checkNotNull(performance.getId())
        val requestIds = requests.mapNotNull(ScheduleModifyCommand::scheduleId)
        val idsToDelete = scheduleRepository.findIdsByPerformanceId(performanceId).filterNot(requestIds::contains)
        deleteSchedules(idsToDelete)

        val schedules = requests.sortedBy { it.scheduleId ?: Long.MAX_VALUE }.map { request ->
            if (request.scheduleId == null) addSchedule(request, performance) else updateSchedule(request, performance)
        }
        val numberedSchedules = scheduleRepository.saveAll(
            scheduleSequenceDomainService.assignScheduleNumbers(schedules),
        )
        val today = LocalDate.now(clock)
        return numberedSchedules.map { schedule ->
            ScheduleResult(
                schedule.getId(),
                schedule.getPerformanceDate(),
                schedule.getTotalTicketCount(),
                calculateDueDate(today, schedule),
                schedule.getScheduleNumber().name,
            )
        }
    }

    private fun addSchedule(request: ScheduleModifyCommand, performance: Performance): Schedule {
        val performanceId = checkNotNull(performance.getId())
        scheduleSequenceDomainService.validateScheduleCount(
            scheduleRepository.countByPerformanceId(performanceId).toLong() + 1,
        )
        return scheduleRepository.save(
            Schedule.createUpcoming(
                request.performanceDate,
                performance.calculateEndAt(request.performanceDate),
                request.totalTicketCount,
                ScheduleNumber.FIRST,
                performanceId,
                LocalDateTime.now(clock),
            ),
        )
    }

    private fun updateSchedule(request: ScheduleModifyCommand, performance: Performance): Schedule {
        val schedule = scheduleRepository.lockById(checkNotNull(request.scheduleId))
            .orElseThrow { FrontofficeApplicationException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND) }
        if (!schedule.belongsTo(checkNotNull(performance.getId()))) {
            throw FrontofficeApplicationException(ScheduleApplicationErrorCode.SCHEDULE_NOT_BELONG_TO_PERFORMANCE)
        }
        return scheduleRepository.save(
            schedule.reschedule(
                request.performanceDate,
                performance.calculateEndAt(request.performanceDate),
                request.totalTicketCount,
                schedule.getScheduleNumber(),
                LocalDateTime.now(clock),
            ),
        )
    }

    private fun deleteSchedules(scheduleIds: List<Long>) {
        if (scheduleIds.isEmpty()) return
        val inactiveStatuses = BookingStatus.inactiveForTicketAllocation()
        if (bookingRepository.existsActiveBookingByScheduleIds(scheduleIds, inactiveStatuses)) {
            throw FrontofficeApplicationException(PerformanceApplicationErrorCode.PERFORMANCE_DELETE_FAILED)
        }
        bookingRepository.deleteInactiveBookingsByScheduleIds(scheduleIds, inactiveStatuses)
        scheduleIds.forEach { scheduleId ->
            val schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow { FrontofficeApplicationException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND) }
            scheduleRepository.delete(schedule)
        }
    }
}
