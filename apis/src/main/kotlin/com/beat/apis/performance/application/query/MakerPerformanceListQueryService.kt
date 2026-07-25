package com.beat.apis.performance.application.query

import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.member.exception.MemberApplicationErrorCode
import com.beat.apis.performance.application.result.MakerPerformanceListResult
import com.beat.apis.performance.application.result.MakerPerformanceResult
import com.beat.apis.performance.application.formatPerformancePeriod
import com.beat.apis.schedule.application.calculateDueDate
import com.beat.contracts.performance.MakerPerformanceListReadPort
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.vo.PerformancePeriod
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Clock

@Service
@Transactional(readOnly = true)
class MakerPerformanceListQueryService(
    private val memberRepository: MemberRepository,
    private val makerPerformanceListReadPort: MakerPerformanceListReadPort,
    private val clock: Clock,
) {
    fun getMemberPerformances(memberId: Long): MakerPerformanceListResult {
        val member = memberRepository.findById(memberId)
            .orElseThrow { ApiApplicationException(MemberApplicationErrorCode.MEMBER_NOT_FOUND) }
        val today = LocalDate.now(clock)
        val details = makerPerformanceListReadPort.findByUserId(member.getUserId()).map { performance ->
            val minDueDate = performance.representativePerformanceDate
                ?.let { calculateDueDate(today, it) }
                ?: Int.MAX_VALUE
            MakerPerformanceResult(
                performance.performanceId,
                performance.genre,
                performance.performanceTitle,
                performance.posterImage,
                formatPerformancePeriod(PerformancePeriod.of(performance.periodStartDate, performance.periodEndDate)),
                minDueDate,
            )
        }
        val sorted = details.filter { it.minDueDate >= 0 }.sortedBy { it.minDueDate } +
            details.filter { it.minDueDate < 0 }.sortedByDescending { it.minDueDate }
        return MakerPerformanceListResult(member.getUserId(), sorted)
    }
}
