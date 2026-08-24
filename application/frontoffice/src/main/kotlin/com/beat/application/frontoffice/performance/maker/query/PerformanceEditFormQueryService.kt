package com.beat.application.frontoffice.performance.maker.query

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.application.frontoffice.performance.CastResult
import com.beat.application.frontoffice.performance.PerformanceImageResult
import com.beat.application.frontoffice.performance.StaffResult
import com.beat.application.frontoffice.performance.exception.PerformanceApplicationErrorCode
import com.beat.application.frontoffice.schedule.calculateDueDate
import com.beat.application.frontoffice.performance.maker.PerformanceMutationResult
import com.beat.application.frontoffice.performance.maker.ScheduleResult
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.repository.PerformanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Clock

@Service
@Transactional(readOnly = true)
class PerformanceEditFormQueryService internal constructor(
    private val memberRepository: MemberRepository,
    private val performanceRepository: PerformanceRepository,
    private val performanceEditFormReader: PerformanceEditFormReader,
    private val clock: Clock,
) {
    fun getPerformanceEdit(memberId: Long, performanceId: Long): PerformanceEditResult {
        return translateDomainFailure {
        val member = memberRepository.findById(memberId)
            ?: throw FrontofficeApplicationException(PerformanceApplicationErrorCode.MEMBER_NOT_FOUND)
        val authoritativePerformance = performanceRepository.findById(performanceId)
            ?: throw FrontofficeApplicationException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND)
        if (!authoritativePerformance.isOwnedBy(member.userId)) {
            throw FrontofficeApplicationException(PerformanceApplicationErrorCode.NOT_PERFORMANCE_OWNER)
        }
        val performance = performanceEditFormReader.findByPerformanceId(performanceId)
            ?: throw FrontofficeApplicationException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND)
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
        PerformanceEditResult(result, performance.hasActiveBooking)
        }
    }
}
