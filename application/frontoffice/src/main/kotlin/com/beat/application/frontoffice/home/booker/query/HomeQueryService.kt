package com.beat.application.frontoffice.home.booker.query

import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.application.frontoffice.schedule.calculateDueDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Service
@Transactional(readOnly = true)
class HomeQueryService internal constructor(
    private val homeProjectionReader: HomeProjectionReader,
    private val clock: Clock,
) {
    fun findHomePerformanceList(genre: String?): HomeFindAllResult {
        return translateDomainFailure {
            val now = LocalDateTime.now(clock)
            val projection = homeProjectionReader.read(genre, now)
            val today = now.toLocalDate()

            HomeFindAllResult(
                promotionList = projection.promotions.map { promotion ->
                    HomePromotionResult(
                        promotionId = promotion.promotionId,
                        promotionPhoto = promotion.promotionPhoto,
                        performanceId = promotion.performanceId,
                        redirectUrl = promotion.redirectUrl,
                        isExternal = promotion.isExternal,
                        carouselNumber = promotion.carouselNumber,
                    )
                },
                performanceList = projection.performances
                    .map { performance ->
                        HomePerformanceResult(
                            performanceId = performance.performanceId,
                            performanceTitle = performance.performanceTitle,
                            performancePeriod = formatPeriod(performance.periodStartDate, performance.periodEndDate),
                            ticketPrice = performance.ticketPrice,
                            dueDate = performance.performanceDate?.let { calculateDueDate(today, it) } ?: Int.MAX_VALUE,
                            genre = performance.genre,
                            posterImage = performance.posterImage,
                            performanceVenue = performance.performanceVenue,
                        )
                    }
                    .sortedWith(compareBy<HomePerformanceResult> { it.dueDate < 0 }.thenBy { abs(it.dueDate) }),
            )
        }
    }

    private fun formatPeriod(startDate: LocalDate, endDate: LocalDate): String {
        val start = startDate.format(DATE_FORMATTER)
        return if (startDate == endDate) start else "$start~${endDate.format(DATE_FORMATTER)}"
    }

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    }
}
