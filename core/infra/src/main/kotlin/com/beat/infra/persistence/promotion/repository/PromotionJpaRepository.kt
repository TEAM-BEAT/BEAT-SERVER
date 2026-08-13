package com.beat.infra.persistence.promotion.repository

import com.beat.domain.promotion.model.CarouselNumber
import com.beat.infra.persistence.promotion.entity.PromotionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

interface PromotionJpaRepository : JpaRepository<PromotionJpaEntity, Long> {

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM Promotion p WHERE p.id IN :promotionIds")
    fun deleteByPromotionIds(@Param("promotionIds") promotionIds: List<Long>)

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM Promotion p WHERE p.performanceId = :performanceId")
    fun deleteByPerformanceId(@Param("performanceId") performanceId: Long?)

    @Query("SELECT p FROM Promotion p WHERE p.carouselNumber = :carouselNumber")
    fun findByCarouselNumber(
        @Param("carouselNumber") carouselNumber: CarouselNumber,
    ): Optional<PromotionJpaEntity>
}
