package com.beat.application.frontoffice.performance.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.application.frontoffice.performance.exception.PerformanceApplicationErrorCode
import com.beat.application.frontoffice.schedule.exception.ScheduleApplicationErrorCode
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.promotion.repository.PromotionRepository
import com.beat.domain.schedule.repository.ScheduleRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PerformanceDeleteCommandService(
    private val performanceRepository: PerformanceRepository,
    private val scheduleRepository: ScheduleRepository,
    private val bookingRepository: BookingRepository,
    private val memberRepository: MemberRepository,
    private val promotionRepository: PromotionRepository,
) {
    @Transactional
    fun deletePerformance(memberId: Long, performanceId: Long) {
        translateDomainFailure {
        val member = memberRepository.findById(memberId)
            ?: throw FrontofficeApplicationException(PerformanceApplicationErrorCode.MEMBER_NOT_FOUND)
        val performance = performanceRepository.lockById(performanceId)
            ?: throw FrontofficeApplicationException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND)
        if (!performance.isOwnedBy(member.userId)) {
            throw FrontofficeApplicationException(PerformanceApplicationErrorCode.NOT_PERFORMANCE_OWNER)
        }

        val scheduleIds = scheduleRepository.findIdsByPerformanceId(performanceId)
        lockSchedules(scheduleIds)
        val inactiveStatuses = BookingStatus.inactiveForTicketAllocation()
        if (scheduleIds.isNotEmpty()) {
            performance.ensureDeletable(
                bookingRepository.existsActiveBookingByScheduleIds(scheduleIds, inactiveStatuses),
            )
            val deletedBookingCount = bookingRepository.deleteInactiveBookingsByScheduleIds(
                scheduleIds,
                inactiveStatuses,
            )
            log.debug { "Deleted ${deletedBookingCount} inactive bookings for performanceId=${performanceId}" }
        }

        scheduleRepository.deleteByPerformanceId(performanceId)
        promotionRepository.deleteByPerformanceId(performanceId)
        performanceRepository.deleteById(checkNotNull(performance.id))
        }
    }

    private fun lockSchedules(scheduleIds: List<Long>) {
        scheduleIds.distinct().sorted().forEach { scheduleId ->
            scheduleRepository.lockById(scheduleId)
                ?: throw FrontofficeApplicationException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND)
        }
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
