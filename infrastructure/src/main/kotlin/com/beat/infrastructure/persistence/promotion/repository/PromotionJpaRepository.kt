package com.beat.infrastructure.persistence.promotion.repository

import com.beat.domain.promotion.model.CarouselNumber
import com.beat.infrastructure.persistence.promotion.entity.PromotionJpaEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

internal interface PromotionJpaRepository : JpaRepository<PromotionJpaEntity, Long> {

    @Query(value = "SELECT GET_LOCK(:lockName, :timeoutSeconds)", nativeQuery = true)
    fun acquireMutationLock(
        @Param("lockName") lockName: String,
        @Param("timeoutSeconds") timeoutSeconds: Int,
    ): Int?

    @Query(value = "SELECT RELEASE_LOCK(:lockName)", nativeQuery = true)
    fun releaseMutationLock(@Param("lockName") lockName: String): Int?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p ORDER BY p.id")
    fun lockAll(): List<PromotionJpaEntity>

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM Promotion p WHERE p.id IN :promotionIds")
    fun deleteByPromotionIds(@Param("promotionIds") promotionIds: List<Long>)

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM Promotion p WHERE p.performanceId = :performanceId")
    fun deleteByPerformanceId(@Param("performanceId") performanceId: Long)

    @Query("SELECT p FROM Promotion p WHERE p.carouselNumber = :carouselNumber")
    fun findByCarouselNumber(
        @Param("carouselNumber") carouselNumber: CarouselNumber,
    ): PromotionJpaEntity?
}
