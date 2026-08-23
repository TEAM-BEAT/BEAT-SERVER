package com.beat.infra.persistence.performance.repository.query

import com.beat.application.frontoffice.performance.maker.query.MakerPerformanceListReader
import com.beat.application.frontoffice.performance.maker.query.MakerPerformanceListItemReadModel
import com.beat.domain.performance.model.Genre
import com.beat.infra.persistence.performance.entity.PerformanceJpaEntity
import com.beat.infra.persistence.performance.entity.PerformancePeriodJpaValue
import com.beat.infra.persistence.schedule.entity.ScheduleJpaEntity
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.extension.createQuery
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
internal class MakerPerformanceListQueries(
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
) : MakerPerformanceListReader {

    override fun findByUserId(userId: Long): List<MakerPerformanceListItemReadModel> {
        val performances = entityManager.createQuery(performanceQuery(userId), jpqlRenderContext).resultList
        if (performances.isEmpty()) {
            return emptyList()
        }

        val representativeDates = findRepresentativeDates(performances.map { checkNotNull(it.performanceId) })
            .associate { it.performanceId to it.performanceDate }

        return performances.map { projection ->
            val performanceId = checkNotNull(projection.performanceId)
            val period = resolvePerformancePeriod(
                performanceId = performanceId,
                startDate = projection.periodStartDate,
                endDate = projection.periodEndDate,
                legacyPeriod = projection.legacyPeriod,
            )
            MakerPerformanceListItemReadModel(
                performanceId = performanceId,
                genre = projection.genre.name,
                performanceTitle = projection.performanceTitle,
                posterImage = projection.posterImage,
                periodStartDate = period.startDate,
                periodEndDate = period.endDate,
                representativePerformanceDate = representativeDates[performanceId],
            )
        }
    }

    private fun performanceQuery(userId: Long) = jpql {
        val performancePeriod = path(PerformanceJpaEntity::performancePeriodValue)
        selectNew<MakerPerformanceProjection>(
            path(PerformanceJpaEntity::id),
            path(PerformanceJpaEntity::genre),
            path(PerformanceJpaEntity::performanceTitle),
            path(PerformanceJpaEntity::posterImage),
            performancePeriod(PerformancePeriodJpaValue::startDate),
            performancePeriod(PerformancePeriodJpaValue::endDate),
            path(PerformanceJpaEntity::legacyPerformancePeriod),
        ).from(
            entity(PerformanceJpaEntity::class),
        ).where(
            path(PerformanceJpaEntity::userId).eq(userId),
        )
    }

    private fun findRepresentativeDates(performanceIds: List<Long>): List<RepresentativePerformanceDateProjection> {
        val now = LocalDateTime.now()
        val query = jpql {
            selectNew<RepresentativePerformanceDateProjection>(
                path(ScheduleJpaEntity::performanceId),
                coalesce(
                    min(
                        caseWhen(path(ScheduleJpaEntity::performanceDate).ge(now))
                            .then(path(ScheduleJpaEntity::performanceDate))
                            .`else`(nullLiteral()),
                    ),
                    min(
                        caseWhen(path(ScheduleJpaEntity::performanceDate).lt(now))
                            .then(path(ScheduleJpaEntity::performanceDate))
                            .`else`(nullLiteral()),
                    ),
                ),
            ).from(
                entity(ScheduleJpaEntity::class),
            ).where(
                path(ScheduleJpaEntity::performanceId).`in`(performanceIds),
            ).groupBy(
                path(ScheduleJpaEntity::performanceId),
            )
        }
        return entityManager.createQuery(query, jpqlRenderContext).resultList
    }

    private data class MakerPerformanceProjection(
        val performanceId: Long?,
        val genre: Genre,
        val performanceTitle: String,
        val posterImage: String,
        val periodStartDate: LocalDate?,
        val periodEndDate: LocalDate?,
        val legacyPeriod: String,
    )

    private data class RepresentativePerformanceDateProjection(
        val performanceId: Long,
        val performanceDate: LocalDateTime,
    )
}
