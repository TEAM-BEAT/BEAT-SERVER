package com.beat.infra.persistence.schedule.repository.query

import com.beat.contracts.schedule.ScheduleReadPort
import com.beat.contracts.schedule.readmodel.MinPerformanceDateReadModel
import com.beat.contracts.schedule.readmodel.ScheduleSummaryReadModel
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.infra.persistence.schedule.entity.ScheduleJpaEntity
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.extension.createQuery
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ScheduleQueries(
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
) : ScheduleReadPort {

    override fun findAllByPerformanceId(performanceId: Long): List<ScheduleSummaryReadModel> {
        val query = jpql {
            selectNew<ScheduleSummaryProjection>(
                path(ScheduleJpaEntity::id),
                path(ScheduleJpaEntity::performanceDate),
                path(ScheduleJpaEntity::totalTicketCount),
                path(ScheduleJpaEntity::soldTicketCount),
                path(ScheduleJpaEntity::scheduleNumber),
            ).from(
                entity(ScheduleJpaEntity::class),
            ).where(
                path(ScheduleJpaEntity::performanceId).eq(performanceId),
            )
        }

        return entityManager.createQuery(query, jpqlRenderContext).resultList.map { projection ->
            ScheduleSummaryReadModel(
                scheduleId = checkNotNull(projection.scheduleId),
                performanceDate = projection.performanceDate,
                totalTicketCount = projection.totalTicketCount,
                soldTicketCount = projection.soldTicketCount,
                scheduleNumber = projection.scheduleNumber.name,
            )
        }
    }

    override fun findMinPerformanceDateByPerformanceIds(
        performanceIds: List<Long>,
    ): List<MinPerformanceDateReadModel> {
        if (performanceIds.isEmpty()) {
            return emptyList()
        }

        val now = LocalDateTime.now()
        val query = jpql {
            selectNew<MinPerformanceDateReadModel>(
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

    private data class ScheduleSummaryProjection(
        val scheduleId: Long?,
        val performanceDate: LocalDateTime,
        val totalTicketCount: Int,
        val soldTicketCount: Int,
        val scheduleNumber: ScheduleNumber,
    )
}
