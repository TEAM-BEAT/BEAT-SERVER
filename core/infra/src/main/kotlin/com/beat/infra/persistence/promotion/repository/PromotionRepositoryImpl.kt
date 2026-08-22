package com.beat.infra.persistence.promotion.repository

import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.model.Promotion
import com.beat.domain.promotion.repository.PromotionRepository
import com.beat.infra.persistence.promotion.mapper.PromotionPersistenceMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Repository
internal class PromotionRepositoryImpl(
    private val promotionJpaRepository: PromotionJpaRepository,
    private val promotionPersistenceMapper: PromotionPersistenceMapper,
) : PromotionRepository {

    override fun findAll(): List<Promotion> =
        promotionJpaRepository.findAll().map(promotionPersistenceMapper::toDomain)

    override fun lockAll(): List<Promotion> {
        check(TransactionSynchronizationManager.isActualTransactionActive()) {
            "Promotion mutation lock requires an active transaction"
        }
        check(promotionJpaRepository.acquireMutationLock(MUTATION_LOCK_NAME, LOCK_TIMEOUT_SECONDS) == 1) {
            "Failed to acquire Promotion mutation lock"
        }
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCompletion(status: Int) {
                check(promotionJpaRepository.releaseMutationLock(MUTATION_LOCK_NAME) == 1) {
                    "Failed to release Promotion mutation lock"
                }
            }
        })
        return promotionJpaRepository.lockAll().map(promotionPersistenceMapper::toDomain)
    }

    override fun findById(promotionId: Long): Promotion? =
        promotionJpaRepository.findById(promotionId)
            .map(promotionPersistenceMapper::toDomain).orElse(null)

    override fun save(promotion: Promotion): Promotion =
        promotionJpaRepository.save(promotionPersistenceMapper.toEntity(promotion))
            .let(promotionPersistenceMapper::toDomain)

    override fun saveAll(promotions: List<Promotion>): List<Promotion> =
        promotionJpaRepository.saveAll(promotions.map(promotionPersistenceMapper::toEntity))
            .map(promotionPersistenceMapper::toDomain)

    override fun deleteByPromotionIds(promotionIds: List<Long>) {
        promotionJpaRepository.deleteByPromotionIds(promotionIds)
    }

    override fun deleteByPerformanceId(performanceId: Long) {
        promotionJpaRepository.deleteByPerformanceId(performanceId)
    }

    override fun findByCarouselNumber(carouselNumber: CarouselNumber): Promotion? =
        promotionJpaRepository.findByCarouselNumber(carouselNumber)?.let(promotionPersistenceMapper::toDomain)

    private companion object {
        const val MUTATION_LOCK_NAME = "beat:promotion:carousel"
        const val LOCK_TIMEOUT_SECONDS = 10
    }
}
