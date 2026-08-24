package com.beat.infrastructure.persistence.home.query

import com.beat.application.frontoffice.home.booker.query.HomePerformanceProjection
import com.beat.application.frontoffice.home.booker.query.HomeProjection
import com.beat.application.frontoffice.home.booker.query.HomeProjectionReader
import com.beat.application.frontoffice.home.booker.query.HomePromotionProjection
import com.beat.domain.performance.model.Genre
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.infrastructure.persistence.performance.entity.PerformanceJpaEntity
import com.beat.infrastructure.persistence.performance.entity.PerformancePeriodJpaValue
import com.beat.infrastructure.persistence.performance.repository.query.resolvePerformancePeriod
import com.beat.infrastructure.persistence.promotion.entity.PromotionJpaEntity
import com.beat.infrastructure.persistence.schedule.entity.ScheduleJpaEntity
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.extension.createQuery
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
internal class HomeProjectionQueries(
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
) : HomeProjectionReader {

    override fun read(genre: String?, now: LocalDateTime): HomeProjection {
        val performances = findPerformances(genre)
        val performanceDates = findPerformanceDates(
            performanceIds = performances.map { checkNotNull(it.performanceId) },
            now = now,
        )
        val promotions = findPromotions()

        return HomeProjection(
            promotions = promotions,
            performances = performances.map { performance ->
                val performanceId = checkNotNull(performance.performanceId)
                val period = resolvePerformancePeriod(
                    performanceId = performanceId,
                    startDate = performance.periodStartDate,
                    endDate = performance.periodEndDate,
                    legacyPeriod = performance.legacyPeriod,
                )
                HomePerformanceProjection(
                    performanceId = performanceId,
                    performanceTitle = performance.performanceTitle,
                    ticketPrice = performance.ticketPrice,
                    genre = performance.genre.name,
                    posterImage = performance.posterImage,
                    performanceVenue = performance.performanceVenue,
                    performanceDate = performanceDates[performanceId],
                    periodStartDate = period.startDate,
                    periodEndDate = period.endDate,
                )
            },
        )
    }

    private fun findPerformances(genre: String?): List<HomePerformanceJpaProjection> {
        val query = jpql {
            val performancePeriod = path(PerformanceJpaEntity::performancePeriodValue)
            selectNew<HomePerformanceJpaProjection>(
                path(PerformanceJpaEntity::id),
                path(PerformanceJpaEntity::performanceTitle),
                path(PerformanceJpaEntity::genre),
                path(PerformanceJpaEntity::ticketPrice),
                path(PerformanceJpaEntity::posterImage),
                path(PerformanceJpaEntity::performanceVenue),
                performancePeriod(PerformancePeriodJpaValue::startDate),
                performancePeriod(PerformancePeriodJpaValue::endDate),
                path(PerformanceJpaEntity::legacyPerformancePeriod),
            ).from(
                entity(PerformanceJpaEntity::class),
            ).apply {
                genre?.let { where(path(PerformanceJpaEntity::genre).eq(Genre.valueOf(it))) }
            }
        }

        return entityManager.createQuery(query, jpqlRenderContext).resultList
    }

    private fun findPerformanceDates(
        performanceIds: List<Long>,
        now: LocalDateTime,
    ): Map<Long, LocalDateTime> {
        if (performanceIds.isEmpty()) {
            return emptyMap()
        }

        val query = jpql {
            selectNew<PerformanceDateProjection>(
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
            .associate { projection ->
                checkNotNull(projection.performanceId) to projection.performanceDate
            }
    }

    private fun findPromotions(): List<HomePromotionProjection> {
        val query = jpql {
            selectNew<HomePromotionJpaProjection>(
                path(PromotionJpaEntity::id),
                path(PromotionJpaEntity::promotionPhoto),
                path(PromotionJpaEntity::performanceId),
                path(PromotionJpaEntity::redirectUrl),
                path(PromotionJpaEntity::isExternal),
                path(PromotionJpaEntity::carouselNumber),
            ).from(
                entity(PromotionJpaEntity::class),
            )
        }

        return entityManager.createQuery(query, jpqlRenderContext).resultList
            .sortedBy { it.carouselNumber.number }
            .map { projection ->
                HomePromotionProjection(
                    promotionId = checkNotNull(projection.promotionId),
                    promotionPhoto = projection.promotionPhoto,
                    performanceId = projection.performanceId,
                    redirectUrl = projection.redirectUrl,
                    isExternal = projection.isExternal,
                    carouselNumber = projection.carouselNumber.name,
                )
            }
    }

    private data class HomePerformanceJpaProjection(
        val performanceId: Long?,
        val performanceTitle: String,
        val genre: Genre,
        val ticketPrice: Int,
        val posterImage: String,
        val performanceVenue: String,
        val periodStartDate: LocalDate?,
        val periodEndDate: LocalDate?,
        val legacyPeriod: String?,
    )

    private data class PerformanceDateProjection(
        val performanceId: Long?,
        val performanceDate: LocalDateTime,
    )

    private data class HomePromotionJpaProjection(
        val promotionId: Long?,
        val promotionPhoto: String,
        val performanceId: Long?,
        val redirectUrl: String,
        val isExternal: Boolean,
        val carouselNumber: CarouselNumber,
    )
}
