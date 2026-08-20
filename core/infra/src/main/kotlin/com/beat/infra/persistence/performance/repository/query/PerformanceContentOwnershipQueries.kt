package com.beat.infra.persistence.performance.repository.query

import com.beat.application.frontoffice.performance.maker.command.PerformanceContentOwnershipReader
import com.beat.infra.persistence.cast.entity.CastJpaEntity
import com.beat.infra.persistence.performanceimage.entity.PerformanceImageJpaEntity
import com.beat.infra.persistence.staff.entity.StaffJpaEntity
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.extension.createQuery
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
internal class PerformanceContentOwnershipQueries(
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
) : PerformanceContentOwnershipReader {

    override fun findPerformanceIdByCastId(castId: Long): Long? =
        entityManager.createQuery(castPerformanceIdQuery(castId), jpqlRenderContext).resultList.firstOrNull()

    override fun findPerformanceIdByStaffId(staffId: Long): Long? =
        entityManager.createQuery(staffPerformanceIdQuery(staffId), jpqlRenderContext).resultList.firstOrNull()

    override fun findPerformanceIdByImageId(imageId: Long): Long? =
        entityManager.createQuery(imagePerformanceIdQuery(imageId), jpqlRenderContext).resultList.firstOrNull()

    private fun castPerformanceIdQuery(castId: Long) = jpql {
        select(path(CastJpaEntity::performanceId)).from(
            entity(CastJpaEntity::class),
        ).where(
            path(CastJpaEntity::id).eq(castId),
        )
    }

    private fun staffPerformanceIdQuery(staffId: Long) = jpql {
        select(path(StaffJpaEntity::performanceId)).from(
            entity(StaffJpaEntity::class),
        ).where(
            path(StaffJpaEntity::id).eq(staffId),
        )
    }

    private fun imagePerformanceIdQuery(imageId: Long) = jpql {
        select(path(PerformanceImageJpaEntity::performanceId)).from(
            entity(PerformanceImageJpaEntity::class),
        ).where(
            path(PerformanceImageJpaEntity::id).eq(imageId),
        )
    }
}
