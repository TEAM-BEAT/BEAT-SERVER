package com.beat.infra.persistence.schedule.repository.query

import com.beat.contracts.schedule.ScheduleReadPort
import com.beat.contracts.schedule.readmodel.MinPerformanceDateReadModel
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

}
