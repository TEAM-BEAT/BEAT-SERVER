package com.beat.infra.persistence.performance.repository.query

import com.beat.contracts.performance.PerformanceSummaryReadPort
import com.beat.contracts.performance.readmodel.PerformanceSummaryReadModel
import com.beat.domain.performance.model.Genre
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.infra.persistence.performance.entity.PaymentAccountJpaValue
import com.beat.infra.persistence.performance.entity.PerformanceJpaEntity
import com.beat.infra.persistence.performance.entity.PerformancePeriodJpaValue
import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.querymodel.jpql.predicate.Predicate
import com.linecorp.kotlinjdsl.querymodel.jpql.select.SelectQuery
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.extension.createQuery
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
class PerformanceSummaryQueries(
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
) : PerformanceSummaryReadPort {

    override fun findById(id: Long): Optional<PerformanceSummaryReadModel> =
        Optional.ofNullable(read(summaryQuery { path(PerformanceJpaEntity::id).eq(id) }).firstOrNull())

    override fun findAllByIds(ids: Collection<Long>): List<PerformanceSummaryReadModel> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        return read(summaryQuery { path(PerformanceJpaEntity::id).`in`(ids) })
    }

    override fun findAll(): List<PerformanceSummaryReadModel> = read(summaryQuery())

    override fun findByGenre(genre: String): List<PerformanceSummaryReadModel> =
        read(summaryQuery { path(PerformanceJpaEntity::genre).eq(Genre.valueOf(genre)) })

    private fun summaryQuery(
        predicate: (Jpql.() -> Predicate)? = null,
    ): SelectQuery<PerformanceSummaryProjection> = jpql {
        val paymentAccount = path(PerformanceJpaEntity::paymentAccount)
        val performancePeriod = path(PerformanceJpaEntity::performancePeriodValue)
        selectNew<PerformanceSummaryProjection>(
            path(PerformanceJpaEntity::id),
            path(PerformanceJpaEntity::userId),
            path(PerformanceJpaEntity::performanceTitle),
            path(PerformanceJpaEntity::genre),
            path(PerformanceJpaEntity::ticketPrice),
            paymentAccount(PaymentAccountJpaValue::bankName),
            paymentAccount(PaymentAccountJpaValue::accountNumber),
            paymentAccount(PaymentAccountJpaValue::accountHolder),
            path(PerformanceJpaEntity::posterImage),
            path(PerformanceJpaEntity::performanceTeamName),
            path(PerformanceJpaEntity::performanceVenue),
            path(PerformanceJpaEntity::performanceContact),
            path(PerformanceJpaEntity::totalScheduleCount),
            performancePeriod(PerformancePeriodJpaValue::startDate),
            performancePeriod(PerformancePeriodJpaValue::endDate),
            path(PerformanceJpaEntity::legacyPerformancePeriod),
        ).from(
            entity(PerformanceJpaEntity::class),
        ).apply {
            predicate?.let { where(it()) }
        }
    }

    private fun read(query: SelectQuery<PerformanceSummaryProjection>): List<PerformanceSummaryReadModel> =
        entityManager.createQuery(query, jpqlRenderContext).resultList.map { projection ->
            val performanceId = checkNotNull(projection.performanceId)
            val period = resolvePerformancePeriod(
                performanceId = performanceId,
                startDate = projection.periodStartDate,
                endDate = projection.periodEndDate,
                legacyPeriod = projection.legacyPeriod,
            )
            PerformanceSummaryReadModel(
                performanceId = performanceId,
                userId = projection.userId,
                performanceTitle = projection.performanceTitle,
                genre = projection.genre.name,
                ticketPrice = projection.ticketPrice,
                bankName = projection.bankName?.name,
                accountNumber = projection.accountNumber,
                accountHolder = projection.accountHolder,
                posterImage = projection.posterImage,
                performanceTeamName = projection.performanceTeamName,
                performanceVenue = projection.performanceVenue,
                performanceContact = projection.performanceContact,
                totalScheduleCount = projection.totalScheduleCount,
                periodStartDate = period.startDate,
                periodEndDate = period.endDate,
            )
        }

    private data class PerformanceSummaryProjection(
        val performanceId: Long?,
        val userId: Long,
        val performanceTitle: String,
        val genre: Genre,
        val ticketPrice: Int,
        val bankName: BankName?,
        val accountNumber: String?,
        val accountHolder: String?,
        val posterImage: String,
        val performanceTeamName: String,
        val performanceVenue: String,
        val performanceContact: String,
        val totalScheduleCount: Int,
        val periodStartDate: LocalDate?,
        val periodEndDate: LocalDate?,
        val legacyPeriod: String,
    )
}
