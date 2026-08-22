package com.beat.application.frontoffice.performance.maker.query

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.application.frontoffice.performance.formatPerformancePeriod
import com.beat.application.frontoffice.performance.exception.PerformanceApplicationErrorCode
import com.beat.application.frontoffice.schedule.calculateDueDate
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
    private val makerPerformanceListReader: MakerPerformanceListReader,
    private val clock: Clock,
) {
    fun getMemberPerformances(memberId: Long): MakerPerformanceListResult {
        return translateDomainFailure {
        val member = memberRepository.findById(memberId)
            ?: throw FrontofficeApplicationException(PerformanceApplicationErrorCode.MEMBER_NOT_FOUND)
        val today = LocalDate.now(clock)
        val details = makerPerformanceListReader.findByUserId(member.userId).map { performance ->
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
        MakerPerformanceListResult(member.userId, sorted)
        }
    }
}
