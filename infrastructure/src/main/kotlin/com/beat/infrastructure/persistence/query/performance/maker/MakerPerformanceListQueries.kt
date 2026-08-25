package com.beat.infrastructure.persistence.query.performance.maker

import com.beat.application.frontoffice.performance.maker.query.MakerPerformanceListItemReadModel
import com.beat.application.frontoffice.performance.maker.query.MakerPerformanceListReader
import com.beat.infrastructure.jooq.generated.Performance
import com.beat.infrastructure.jooq.generated.Schedule
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.Clock
import java.time.LocalDateTime

@Repository
internal class MakerPerformanceListQueries(
    private val dsl: DSLContext,
    private val clock: Clock,
) : MakerPerformanceListReader {

    override fun findByUserId(userId: Long): List<MakerPerformanceListItemReadModel> {
        val performances = dsl.select(
            Performance.ID,
            Performance.GENRE,
            Performance.PERFORMANCE_TITLE,
            Performance.POSTER_IMAGE,
            Performance.PERFORMANCE_START_DATE,
            Performance.PERFORMANCE_END_DATE,
        ).from(Performance.TABLE)
            .where(Performance.USER_ID.eq(userId))
            .fetch { record ->
                MakerPerformanceProjection(
                    performanceId = record.get(Performance.ID),
                    genre = record.get(Performance.GENRE)!!,
                    performanceTitle = record.get(Performance.PERFORMANCE_TITLE)!!,
                    posterImage = record.get(Performance.POSTER_IMAGE)!!,
                    periodStartDate = record.get(Performance.PERFORMANCE_START_DATE),
                    periodEndDate = record.get(Performance.PERFORMANCE_END_DATE),
                )
            }

        if (performances.isEmpty()) {
            return emptyList()
        }

        val representativeDates = findRepresentativeDates(performances.mapNotNull { it.performanceId })
            .associate { it.performanceId to it.performanceDate }

        return performances.map { projection ->
            val performanceId = checkNotNull(projection.performanceId)
            val period = com.beat.domain.performance.vo.PerformancePeriod.of(projection.periodStartDate!!, projection.periodEndDate!!)
            MakerPerformanceListItemReadModel(
                performanceId = performanceId,
                genre = projection.genre,
                performanceTitle = projection.performanceTitle,
                posterImage = projection.posterImage,
                periodStartDate = period.startDate,
                periodEndDate = period.endDate,
                representativePerformanceDate = representativeDates[performanceId],
            )
        }
    }

    private fun findRepresentativeDates(performanceIds: List<Long>): List<RepresentativePerformanceDateProjection> {
        val now = LocalDateTime.now(clock)
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
                RepresentativePerformanceDateProjection(
                    performanceId = record.get(Schedule.PERFORMANCE_ID)!!,
                    performanceDate = record.get("performance_date", LocalDateTime::class.java)!!,
                )
            }
    }

    private data class MakerPerformanceProjection(
        val performanceId: Long?,
        val genre: String,
        val performanceTitle: String,
        val posterImage: String,
        val periodStartDate: java.time.LocalDate?,
        val periodEndDate: java.time.LocalDate?,
    )

    private data class RepresentativePerformanceDateProjection(
        val performanceId: Long,
        val performanceDate: LocalDateTime,
    )
}
