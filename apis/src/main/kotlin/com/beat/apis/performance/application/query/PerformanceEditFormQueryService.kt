package com.beat.apis.performance.application.query

import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.member.exception.MemberApplicationErrorCode
import com.beat.apis.performance.application.result.CastResult
import com.beat.apis.performance.application.result.PerformanceEditResult
import com.beat.apis.performance.application.result.PerformanceImageResult
import com.beat.apis.performance.application.result.PerformanceMutationResult
import com.beat.apis.performance.application.result.ScheduleResult
import com.beat.apis.performance.application.result.StaffResult
import com.beat.apis.performance.exception.PerformanceApplicationErrorCode
import com.beat.apis.schedule.application.calculateDueDate
import com.beat.contracts.performance.PerformanceEditFormReadPort
import com.beat.domain.member.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Clock

@Service
@Transactional(readOnly = true)
class PerformanceEditFormQueryService(
    private val memberRepository: MemberRepository,
    private val performanceEditFormReadPort: PerformanceEditFormReadPort,
    private val clock: Clock,
) {
    fun getPerformanceEdit(memberId: Long, performanceId: Long): PerformanceEditResult {
        val member = memberRepository.findById(memberId)
            .orElseThrow { ApiApplicationException(MemberApplicationErrorCode.MEMBER_NOT_FOUND) }
        val performance = performanceEditFormReadPort.findByPerformanceId(performanceId)
            .orElseThrow { ApiApplicationException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND) }
        if (performance.userId != member.getUserId()) {
            throw ApiApplicationException(PerformanceApplicationErrorCode.NOT_PERFORMANCE_OWNER)
        }
        val today = LocalDate.now(clock)
        val schedules = performance.schedules.map { schedule ->
            ScheduleResult(
                schedule.id,
                schedule.performanceDate,
                schedule.totalTicketCount,
                calculateDueDate(today, schedule.performanceDate),
                schedule.scheduleNumber,
            )
        }
        val casts = performance.casts.map { CastResult(it.id, it.name, it.role, it.photo) }
        val staffs = performance.staffs.map { StaffResult(it.id, it.name, it.role, it.photo) }
        val images = performance.images.map { PerformanceImageResult(it.id, it.url) }

        val result = PerformanceMutationResult(
            userId = performance.userId,
            performanceId = performance.performanceId,
            performanceTitle = performance.performanceTitle,
            genre = performance.genre,
            runningTime = performance.runningTime,
            performanceDescription = performance.performanceDescription,
            performanceAttentionNote = performance.performanceAttentionNote,
            bankName = performance.bankName,
            accountNumber = performance.accountNumber,
            accountHolder = performance.accountHolder,
            posterImage = performance.posterImage,
            performanceTeamName = performance.performanceTeamName,
            performanceVenue = performance.performanceVenue,
            roadAddressName = performance.roadAddressName,
            placeDetailAddress = performance.placeDetailAddress,
            latitude = performance.latitude,
            longitude = performance.longitude,
            performanceContact = performance.performanceContact,
            performancePeriod = performance.performancePeriod,
            ticketPrice = performance.ticketPrice,
            totalScheduleCount = performance.totalScheduleCount,
            schedules = schedules,
            casts = casts,
            staffs = staffs,
            images = images,
        )
        return PerformanceEditResult(result, performance.hasActiveBooking)
    }
}
