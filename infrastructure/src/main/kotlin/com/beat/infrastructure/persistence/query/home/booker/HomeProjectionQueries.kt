package com.beat.infrastructure.persistence.query.home.booker

import com.beat.application.frontoffice.home.booker.query.HomePerformanceProjection
import com.beat.application.frontoffice.home.booker.query.HomeProjection
import com.beat.application.frontoffice.home.booker.query.HomeProjectionReader
import com.beat.application.frontoffice.home.booker.query.HomePromotionProjection
import com.beat.infrastructure.jooq.generated.Performance
import com.beat.infrastructure.jooq.generated.Promotion
import com.beat.infrastructure.jooq.generated.Schedule
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
internal class HomeProjectionQueries(
    private val dsl: DSLContext,
) : HomeProjectionReader {

    override fun read(genre: String?, now: LocalDateTime): HomeProjection {
        val performances = findPerformances(genre)
        val performanceDates = findPerformanceDates(
            performanceIds = performances.mapNotNull { it.performanceId },
            now = now,
        )
        val promotions = findPromotions()

        return HomeProjection(
            promotions = promotions,
            performances = performances.map { performance ->
                val performanceId = checkNotNull(performance.performanceId)
                val period = com.beat.domain.performance.vo.PerformancePeriod.of(performance.periodStartDate!!, performance.periodEndDate!!)
                HomePerformanceProjection(
                    performanceId = performanceId,
                    performanceTitle = performance.performanceTitle,
                    ticketPrice = performance.ticketPrice,
                    genre = performance.genre,
                    posterImage = performance.posterImage,
                    performanceVenue = performance.performanceVenue,
                    performanceDate = performanceDates[performanceId],
                    periodStartDate = period.startDate,
                    periodEndDate = period.endDate,
                )
            },
        )
    }

    private fun findPerformances(genre: String?): List<HomePerformanceProjectionRow> {
        val query = dsl.select(
            Performance.ID,
            Performance.PERFORMANCE_TITLE,
            Performance.GENRE,
            Performance.TICKET_PRICE,
            Performance.POSTER_IMAGE,
            Performance.PERFORMANCE_VENUE,
            Performance.PERFORMANCE_START_DATE,
            Performance.PERFORMANCE_END_DATE,
        ).from(Performance.TABLE)

        if (genre != null) {
            query.where(Performance.GENRE.eq(genre))
        }

        return query.fetch { record ->
            HomePerformanceProjectionRow(
                performanceId = record.get(Performance.ID),
                performanceTitle = record.get(Performance.PERFORMANCE_TITLE)!!,
                genre = record.get(Performance.GENRE)!!,
                ticketPrice = record.get(Performance.TICKET_PRICE)!!,
                posterImage = record.get(Performance.POSTER_IMAGE)!!,
                performanceVenue = record.get(Performance.PERFORMANCE_VENUE)!!,
                periodStartDate = record.get(Performance.PERFORMANCE_START_DATE),
                periodEndDate = record.get(Performance.PERFORMANCE_END_DATE),
            )
        }
    }

    private fun findPerformanceDates(
        performanceIds: List<Long>,
        now: LocalDateTime,
    ): Map<Long, LocalDateTime> {
        if (performanceIds.isEmpty()) {
            return emptyMap()
        }

        val futureMin = DSL.min(
            DSL.case_()
                .`when`(Schedule.PERFORMANCE_DATE.ge(DSL.`val`(now)), Schedule.PERFORMANCE_DATE)
                .otherwise(DSL.inline(null, org.jooq.impl.SQLDataType.LOCALDATETIME)),
        )
        val pastMin = DSL.min(
            DSL.case_()
                .`when`(Schedule.PERFORMANCE_DATE.lt(DSL.`val`(now)), Schedule.PERFORMANCE_DATE)
                .otherwise(DSL.inline(null, org.jooq.impl.SQLDataType.LOCALDATETIME)),
        )
        val coalesced = DSL.coalesce(futureMin, pastMin).`as`("performance_date")

        return dsl.select(
            Schedule.PERFORMANCE_ID,
            coalesced,
        ).from(Schedule.TABLE)
            .where(Schedule.PERFORMANCE_ID.`in`(performanceIds))
            .groupBy(Schedule.PERFORMANCE_ID)
            .fetch { record ->
                val pid = record.get(Schedule.PERFORMANCE_ID)!!
                val date = record.get("performance_date", LocalDateTime::class.java)
                pid to date
            }
            .filter { it.second != null }
            .associate { it.first to it.second!! }
            // Performances without schedules will be missing here, caller handles null
            .let { map ->
                // Need to ensure all requested ids present? Original returns only those with schedules.
                // For missing, we return null later.
                map
            }
    }

    private fun findPromotions(): List<HomePromotionProjection> {
        val rows = dsl.select(
            Promotion.ID,
            Promotion.PROMOTION_PHOTO,
            Promotion.PERFORMANCE_ID,
            Promotion.REDIRECT_URL,
            Promotion.IS_EXTERNAL,
            Promotion.CAROUSEL_NUMBER,
        ).from(Promotion.TABLE)
            .fetch { record ->
                HomePromotionRow(
                    promotionId = record.get(Promotion.ID),
                    promotionPhoto = record.get(Promotion.PROMOTION_PHOTO)!!,
                    performanceId = record.get(Promotion.PERFORMANCE_ID),
                    redirectUrl = record.get(Promotion.REDIRECT_URL)!!,
                    isExternal = record.get(Promotion.IS_EXTERNAL)!!,
                    carouselNumber = record.get(Promotion.CAROUSEL_NUMBER)!!,
                )
            }

        // Preserve original sort by carouselNumber enum order (ONE, TWO, THREE...)
        // CarouselNumber number ordering: use lexical via enum name ordering? Original used CarouselNumber.number int.
        // We replicate by mapping to enum and sorting by enum's number via valueOf.
        return rows.sortedBy { row ->
            try {
                com.beat.domain.promotion.model.CarouselNumber.valueOf(row.carouselNumber).number
            } catch (_: Exception) {
                Int.MAX_VALUE
            }
        }.map { row ->
            HomePromotionProjection(
                promotionId = checkNotNull(row.promotionId),
                promotionPhoto = row.promotionPhoto,
                performanceId = row.performanceId,
                redirectUrl = row.redirectUrl,
                isExternal = row.isExternal,
                carouselNumber = row.carouselNumber,
            )
        }
    }

    private data class HomePerformanceProjectionRow(
        val performanceId: Long?,
        val performanceTitle: String,
        val genre: String,
        val ticketPrice: Int,
        val posterImage: String,
        val performanceVenue: String,
        val periodStartDate: java.time.LocalDate?,
        val periodEndDate: java.time.LocalDate?,
    )

    private data class HomePromotionRow(
        val promotionId: Long?,
        val promotionPhoto: String,
        val performanceId: Long?,
        val redirectUrl: String,
        val isExternal: Boolean,
        val carouselNumber: String,
    )
}
