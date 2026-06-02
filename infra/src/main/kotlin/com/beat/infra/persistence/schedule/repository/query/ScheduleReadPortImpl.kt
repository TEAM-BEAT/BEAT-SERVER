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
class ScheduleReadPortImpl(
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

        // 공연별 대표 일자: 미래 공연이 있으면 가장 가까운 미래 일자, 없으면 가장 최근 과거 일자.
        // 기존 JPQL 의 CASE WHEN MIN(미래) IS NOT NULL THEN MIN(미래) ELSE MIN(과거) 와 동일한
        // 의미를 COALESCE(MIN(미래), MIN(과거)) 로 표현한다.
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
