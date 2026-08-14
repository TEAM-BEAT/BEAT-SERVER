package com.beat.infra.persistence.promotion.repository.query

import com.beat.contracts.promotion.HomePromotionReadPort
import com.beat.contracts.promotion.readmodel.HomePromotionReadModel
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.infra.persistence.promotion.entity.PromotionJpaEntity
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.extension.createQuery
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
class HomePromotionQueries(
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
) : HomePromotionReadPort {

    override fun findAllOrdered(): List<HomePromotionReadModel> {
        val query = jpql {
            selectNew<HomePromotionProjection>(
                path(PromotionJpaEntity::id),
                path(PromotionJpaEntity::promotionPhoto),
                path(PromotionJpaEntity::performanceId),
                path(PromotionJpaEntity::redirectUrl),
                path(PromotionJpaEntity::isExternal),
                path(PromotionJpaEntity::carouselNumber),
            ).from(
                entity(PromotionJpaEntity::class),
            ).orderBy(
                path(PromotionJpaEntity::carouselNumber).asc(),
            )
        }

        return entityManager.createQuery(query, jpqlRenderContext).resultList.map { projection ->
            HomePromotionReadModel(
                promotionId = checkNotNull(projection.promotionId),
                promotionPhoto = projection.promotionPhoto,
                performanceId = projection.performanceId,
                redirectUrl = projection.redirectUrl,
                external = projection.external,
                carouselNumber = projection.carouselNumber.name,
            )
        }
    }

    private data class HomePromotionProjection(
        val promotionId: Long?,
        val promotionPhoto: String,
        val performanceId: Long?,
        val redirectUrl: String,
        val external: Boolean,
        val carouselNumber: CarouselNumber,
    )
}
