package com.beat.apis.home.application.query

import com.beat.apis.home.application.result.HomePerformanceResult
import com.beat.apis.home.application.result.HomePromotionResult
import com.beat.apis.home.application.result.HomeFindAllResult
import com.beat.apis.schedule.application.calculateDueDate
import com.beat.contracts.performance.PerformanceSummaryReadPort
import com.beat.contracts.performance.readmodel.PerformanceSummaryReadModel
import com.beat.contracts.promotion.HomePromotionReadPort
import com.beat.contracts.schedule.ScheduleReadPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Service
@Transactional(readOnly = true)
class HomeQueryService(
    private val performanceSummaryReadPort: PerformanceSummaryReadPort,
    private val scheduleReadPort: ScheduleReadPort,
    private val homePromotionReadPort: HomePromotionReadPort,
    private val clock: Clock,
) {
    fun findHomePerformanceList(genre: String?): HomeFindAllResult {
        val performances = findPerformances(genre)
        val promotions = findPromotions()
        return HomeFindAllResult(
            promotionList = promotions,
            performanceList = createPerformanceResults(performances),
        )
    }

    private fun createPerformanceResults(performances: List<PerformanceSummaryReadModel>): List<HomePerformanceResult> {
        if (performances.isEmpty()) {
            return emptyList()
        }

        val performanceDateMap = mutableMapOf<Long, LocalDateTime>()
        scheduleReadPort
            .findMinPerformanceDateByPerformanceIds(performances.map { it.performanceId })
            .forEach { performanceDateMap.putIfAbsent(it.performanceId, it.performanceDate) }
        val today = LocalDate.now(clock)

        return performances
            .map { performance -> createResult(today, performance, performanceDateMap) }
            .sortedWith(compareBy<HomePerformanceResult> { it.dueDate < 0 }.thenBy { abs(it.dueDate) })
    }

    private fun findPromotions(): List<HomePromotionResult> = homePromotionReadPort.findAllOrdered()
        .map { promotion ->
            HomePromotionResult(
                promotionId = promotion.promotionId,
                promotionPhoto = promotion.promotionPhoto,
                performanceId = promotion.performanceId,
                redirectUrl = promotion.redirectUrl,
                isExternal = promotion.external,
                carouselNumber = promotion.carouselNumber,
            )
        }

    private fun findPerformances(genre: String?): List<PerformanceSummaryReadModel> =
        genre?.let(performanceSummaryReadPort::findByGenre)
            ?: performanceSummaryReadPort.findAll()

    private fun createResult(
        today: LocalDate,
        performance: PerformanceSummaryReadModel,
        performanceDateMap: Map<Long, LocalDateTime>,
    ): HomePerformanceResult = HomePerformanceResult(
        performanceId = performance.performanceId,
        performanceTitle = performance.performanceTitle,
        performancePeriod = formatPeriod(performance.periodStartDate, performance.periodEndDate),
        ticketPrice = performance.ticketPrice,
        dueDate = performanceDateMap[performance.performanceId]?.let { calculateDueDate(today, it) } ?: Int.MAX_VALUE,
        genre = performance.genre,
        posterImage = performance.posterImage,
        performanceVenue = performance.performanceVenue,
    )

    private fun formatPeriod(startDate: LocalDate, endDate: LocalDate): String {
        val start = startDate.format(DATE_FORMATTER)
        return if (startDate == endDate) start else "$start~${endDate.format(DATE_FORMATTER)}"
    }

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    }
}
