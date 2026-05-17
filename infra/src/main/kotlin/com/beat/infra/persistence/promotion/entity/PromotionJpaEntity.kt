package com.beat.infra.persistence.promotion.entity

import com.beat.domain.promotion.domain.CarouselNumber
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "Promotion")
@Table(name = "promotion")
class PromotionJpaEntity private constructor(
    id: Long?,
    promotionPhoto: String,
    performanceId: Long?,
    redirectUrl: String,
    isExternal: Boolean,
    carouselNumber: CarouselNumber,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    var id: Long? = id
        protected set

    @Column(nullable = false)
    var promotionPhoto: String = promotionPhoto
        protected set

    @Column(name = "performance_id", nullable = true)
    var performanceId: Long? = performanceId
        protected set

    @Column(nullable = false)
    var redirectUrl: String = redirectUrl
        protected set

    @Column(nullable = false)
    var isExternal: Boolean = isExternal
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var carouselNumber: CarouselNumber = carouselNumber
        protected set

    companion object {
        @JvmStatic
        fun rehydrate(
            id: Long?,
            promotionPhoto: String,
            performanceId: Long?,
            redirectUrl: String,
            isExternal: Boolean,
            carouselNumber: CarouselNumber,
        ): PromotionJpaEntity = PromotionJpaEntity(
            id = id,
            promotionPhoto = promotionPhoto,
            performanceId = performanceId,
            redirectUrl = redirectUrl,
            isExternal = isExternal,
            carouselNumber = carouselNumber,
        )
    }
}
